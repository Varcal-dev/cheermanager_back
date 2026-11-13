package com.varcal.cheermanager.Service.Persona;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.DocumentoDeportista;
import com.varcal.cheermanager.Models.Personas.DocumentoDeportista.Tipo;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.repository.Personas.DocumentoDeportistaRepository;

/** C4: vencimientos de certificado medico, EPS, seguro y licencia, con alertas. */
@Service
public class DocumentoDeportistaService {

    public enum Estado { VENCIDO, POR_VENCER, SIN_REGISTRO, VIGENTE }

    public record Solicitud(Tipo tipo, String entidad, String numero, LocalDate fechaEmision,
            LocalDate fechaVencimiento, String observaciones) {
    }

    public record Documento(Integer id, Tipo tipo, String entidad, String numero, LocalDate fechaEmision,
            LocalDate fechaVencimiento, String observaciones, LocalDateTime fechaRegistro) {
        static Documento de(DocumentoDeportista d) {
            return new Documento(d.getId(), d.getTipo(), d.getEntidad(), d.getNumero(), d.getFechaEmision(),
                    d.getFechaVencimiento(), d.getObservaciones(), d.getFechaRegistro());
        }
    }

    public record Alerta(Integer deportistaId, Integer personaId, String nombre, Tipo tipo, Estado estado,
            LocalDate fechaVencimiento, Integer diasRestantes, String entidad) {
    }

    public record Alertas(int vencidos, int porVencer, int sinRegistro, List<Alerta> items) {
    }

    /** Tipos que todo deportista activo debe tener; los demas solo alertan si ya fueron registrados. */
    public static final Set<Tipo> REQUERIDOS_POR_DEFECTO = Set.of(Tipo.CERTIFICADO_MEDICO, Tipo.EPS);

    private final DocumentoDeportistaRepository repo;
    private final DeportistaRepository deportistaRepository;

    public DocumentoDeportistaService(DocumentoDeportistaRepository repo, DeportistaRepository deportistaRepository) {
        this.repo = repo;
        this.deportistaRepository = deportistaRepository;
    }

    protected LocalDate hoy() {
        return com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy();
    }

    @Transactional
    public Documento registrar(Integer deportistaId, Solicitud s, int usuarioId) {
        if (s.tipo() == null) {
            throw new IllegalArgumentException("Indica el tipo de documento");
        }
        if (deportistaRepository.findById(deportistaId).isEmpty()) {
            throw new IllegalArgumentException("Deportista no encontrado");
        }
        LocalDate hoy = hoy();
        if (s.fechaEmision() != null && s.fechaEmision().isAfter(hoy)) {
            throw new IllegalArgumentException("La fecha de emision no puede ser futura");
        }
        if (s.fechaEmision() != null && s.fechaVencimiento() != null && s.fechaVencimiento().isBefore(s.fechaEmision())) {
            throw new IllegalArgumentException("El vencimiento no puede ser anterior a la emision");
        }
        if (s.fechaVencimiento() == null && s.tipo() != Tipo.EPS && s.tipo() != Tipo.SEGURO) {
            throw new IllegalArgumentException("Este documento necesita fecha de vencimiento");
        }
        DocumentoDeportista d = new DocumentoDeportista();
        d.setDeportistaId(deportistaId);
        d.setTipo(s.tipo());
        d.setEntidad(recortar(s.entidad(), 120, "La entidad"));
        d.setNumero(recortar(s.numero(), 60, "El numero"));
        d.setObservaciones(recortar(s.observaciones(), 300, "Las observaciones"));
        d.setFechaEmision(s.fechaEmision());
        d.setFechaVencimiento(s.fechaVencimiento());
        d.setRegistradoPor(usuarioId);
        d.setFechaRegistro(LocalDateTime.now());
        return Documento.de(repo.save(d));
    }

    public List<Documento> historial(Integer deportistaId) {
        return repo.findByDeportistaIdOrderByIdDesc(deportistaId).stream().map(Documento::de).toList();
    }

    /**
     * @param dias       ventana de aviso: un documento "por vencer" vence dentro de esos dias
     * @param requeridos tipos que deben existir para cada deportista activo (null = certificado medico y EPS)
     * @param todos      incluir tambien los vigentes (para poder registrar documentos desde la lista)
     */
    public Alertas alertas(int dias, Set<Tipo> requeridos, boolean todos) {
        if (dias < 0 || dias > 365) {
            throw new IllegalArgumentException("La ventana de aviso va de 0 a 365 dias");
        }
        Set<Tipo> exigidos = requeridos == null ? REQUERIDOS_POR_DEFECTO : requeridos;
        LocalDate hoy = hoy();
        // El ultimo registro de cada deportista y tipo es el que vale.
        Map<String, DocumentoDeportista> ultimo = new HashMap<>();
        for (DocumentoDeportista d : repo.findAllByOrderByIdDesc()) {
            ultimo.putIfAbsent(d.getDeportistaId() + "|" + d.getTipo(), d);
        }
        List<Alerta> items = new ArrayList<>();
        int venc = 0, por = 0, sin = 0;
        for (Deportista dep : deportistaRepository.findAll()) {
            if (dep.getEstado() == null || !"Activo".equalsIgnoreCase(dep.getEstado().getEstado())) {
                continue;
            }
            Persona p = dep.getPersona();
            String nombre = p.getNombre() + " " + p.getApellidos();
            for (Tipo t : Tipo.values()) {
                DocumentoDeportista d = ultimo.get(dep.getId() + "|" + t);
                if (d == null && !exigidos.contains(t)) {
                    continue;
                }
                Estado e = estado(d, hoy, dias);
                if (e == Estado.VIGENTE && !todos) {
                    continue;
                }
                if (e == Estado.VENCIDO) venc++;
                else if (e == Estado.POR_VENCER) por++;
                else if (e == Estado.SIN_REGISTRO) sin++;
                LocalDate fv = d == null ? null : d.getFechaVencimiento();
                items.add(new Alerta(dep.getId(), p.getId(), nombre, t, e, fv,
                        fv == null ? null : (int) ChronoUnit.DAYS.between(hoy, fv),
                        d == null ? null : d.getEntidad()));
            }
        }
        items.sort(Comparator.comparing((Alerta a) -> a.estado().ordinal())
                .thenComparing(a -> a.diasRestantes() == null ? Integer.MAX_VALUE : a.diasRestantes())
                .thenComparing(Alerta::nombre));
        return new Alertas(venc, por, sin, items);
    }

    static Estado estado(DocumentoDeportista d, LocalDate hoy, int dias) {
        if (d == null) {
            return Estado.SIN_REGISTRO;
        }
        LocalDate v = d.getFechaVencimiento();
        if (v == null) {
            return Estado.VIGENTE;
        }
        if (v.isBefore(hoy)) {
            return Estado.VENCIDO;
        }
        return ChronoUnit.DAYS.between(hoy, v) <= dias ? Estado.POR_VENCER : Estado.VIGENTE;
    }

    private static String recortar(String s, int max, String campo) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.trim();
        if (t.length() > max) {
            throw new IllegalArgumentException(campo + " no puede superar " + max + " caracteres");
        }
        return t;
    }
}
