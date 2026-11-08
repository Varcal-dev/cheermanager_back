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

import com.varcal.cheermanager.Models.Personas.CertificacionEntrenador;
import com.varcal.cheermanager.Models.Personas.CertificacionEntrenador.Tipo;
import com.varcal.cheermanager.Models.Personas.Entrenador;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.repository.Personas.CertificacionEntrenadorRepository;
import com.varcal.cheermanager.repository.Personas.EntrenadorRepository;

/** C10: certificaciones de entrenadores con vencimiento y alertas. */
@Service
public class CertificacionEntrenadorService {

    public enum Estado { VENCIDA, POR_VENCER, SIN_REGISTRO, VIGENTE }

    public record Solicitud(Tipo tipo, String nombre, String entidad, LocalDate fechaEmision,
            LocalDate fechaVencimiento) {
    }

    public record Certificacion(Integer id, Tipo tipo, String nombre, String entidad, LocalDate fechaEmision,
            LocalDate fechaVencimiento, LocalDateTime fechaRegistro) {
        static Certificacion de(CertificacionEntrenador c) {
            return new Certificacion(c.getId(), c.getTipo(), c.getNombre(), c.getEntidad(), c.getFechaEmision(),
                    c.getFechaVencimiento(), c.getFechaRegistro());
        }
    }

    public record Alerta(Integer entrenadorId, String nombre, Tipo tipo, String certificacion, Estado estado,
            LocalDate fechaVencimiento, Integer diasRestantes) {
    }

    public record Alertas(int vencidas, int porVencer, int sinRegistro, List<Alerta> items) {
    }

    /** Toda persona que entrena debe tener al menos estas dos vigentes. */
    public static final Set<Tipo> REQUERIDAS = Set.of(Tipo.PRIMEROS_AUXILIOS, Tipo.SEGURIDAD_DEPORTIVA);

    private final CertificacionEntrenadorRepository repo;
    private final EntrenadorRepository entrenadorRepository;

    public CertificacionEntrenadorService(CertificacionEntrenadorRepository repo,
            EntrenadorRepository entrenadorRepository) {
        this.repo = repo;
        this.entrenadorRepository = entrenadorRepository;
    }

    protected LocalDate hoy() {
        return LocalDate.now();
    }

    @Transactional
    public Certificacion registrar(Integer entrenadorId, Solicitud s, int usuarioId) {
        if (s.tipo() == null) {
            throw new IllegalArgumentException("Indica el tipo de certificacion");
        }
        if (entrenadorRepository.findById(entrenadorId).isEmpty()) {
            throw new IllegalArgumentException("Entrenador no encontrado");
        }
        boolean pideNombre = s.tipo() == Tipo.CURSO_FEDERACION || s.tipo() == Tipo.OTRA;
        String nombre = recortar(s.nombre(), 120, "El nombre");
        if (pideNombre && nombre == null) {
            throw new IllegalArgumentException("Escribe el nombre del curso o certificacion");
        }
        if (s.fechaEmision() != null && s.fechaEmision().isAfter(hoy())) {
            throw new IllegalArgumentException("La fecha de emision no puede ser futura");
        }
        if (s.fechaEmision() != null && s.fechaVencimiento() != null && s.fechaVencimiento().isBefore(s.fechaEmision())) {
            throw new IllegalArgumentException("El vencimiento no puede ser anterior a la emision");
        }
        if (s.fechaVencimiento() == null && !pideNombre) {
            throw new IllegalArgumentException("Esta certificacion necesita fecha de vencimiento");
        }
        CertificacionEntrenador c = new CertificacionEntrenador();
        c.setEntrenadorId(entrenadorId);
        c.setTipo(s.tipo());
        c.setNombre(nombre);
        c.setEntidad(recortar(s.entidad(), 120, "La entidad"));
        c.setFechaEmision(s.fechaEmision());
        c.setFechaVencimiento(s.fechaVencimiento());
        c.setRegistradoPor(usuarioId);
        c.setFechaRegistro(LocalDateTime.now());
        return Certificacion.de(repo.save(c));
    }

    public List<Certificacion> historial(Integer entrenadorId) {
        return repo.findByEntrenadorIdOrderByIdDesc(entrenadorId).stream().map(Certificacion::de).toList();
    }

    /** Clave de "lo mismo": un curso de federacion u otro se distingue por nombre; los demas, por tipo. */
    static String clave(Integer entrenadorId, Tipo t, String nombre) {
        boolean porNombre = t == Tipo.CURSO_FEDERACION || t == Tipo.OTRA;
        return entrenadorId + "|" + t + (porNombre && nombre != null ? "|" + nombre.trim().toLowerCase() : "");
    }

    public Alertas alertas(int dias, boolean todos) {
        if (dias < 0 || dias > 365) {
            throw new IllegalArgumentException("La ventana de aviso va de 0 a 365 dias");
        }
        LocalDate hoy = hoy();
        Map<String, CertificacionEntrenador> ultimas = new HashMap<>();
        for (CertificacionEntrenador c : repo.findAllByOrderByIdDesc()) {
            ultimas.putIfAbsent(clave(c.getEntrenadorId(), c.getTipo(), c.getNombre()), c);
        }
        List<Alerta> items = new ArrayList<>();
        int venc = 0, por = 0, sin = 0;
        for (Entrenador e : entrenadorRepository.findAll()) {
            if (e.getEstado() == null || !"Activo".equalsIgnoreCase(e.getEstado().getEstado())) {
                continue;
            }
            Persona p = e.getPersona();
            String nombre = p.getNombre() + " " + p.getApellidos();
            // Requeridas (existan o no) + cualquier otra que ya tenga registrada.
            List<CertificacionEntrenador> propias = new ArrayList<>();
            for (Tipo t : REQUERIDAS) {
                CertificacionEntrenador c = ultimas.get(clave(e.getId(), t, null));
                if (c == null) {
                    sin++;
                    items.add(new Alerta(e.getId(), nombre, t, null, Estado.SIN_REGISTRO, null, null));
                } else {
                    propias.add(c);
                }
            }
            for (CertificacionEntrenador c : ultimas.values()) {
                if (c.getEntrenadorId().equals(e.getId()) && !REQUERIDAS.contains(c.getTipo())) {
                    propias.add(c);
                }
            }
            for (CertificacionEntrenador c : propias) {
                Estado est = estado(c, hoy, dias);
                if (est == Estado.VIGENTE && !todos) {
                    continue;
                }
                if (est == Estado.VENCIDA) venc++;
                else if (est == Estado.POR_VENCER) por++;
                LocalDate fv = c.getFechaVencimiento();
                items.add(new Alerta(e.getId(), nombre, c.getTipo(), c.getNombre(), est, fv,
                        fv == null ? null : (int) ChronoUnit.DAYS.between(hoy, fv)));
            }
        }
        items.sort(Comparator.comparing((Alerta a) -> a.estado().ordinal())
                .thenComparing(a -> a.diasRestantes() == null ? Integer.MAX_VALUE : a.diasRestantes())
                .thenComparing(Alerta::nombre));
        return new Alertas(venc, por, sin, items);
    }

    static Estado estado(CertificacionEntrenador c, LocalDate hoy, int dias) {
        LocalDate v = c.getFechaVencimiento();
        if (v == null) {
            return Estado.VIGENTE;
        }
        if (v.isBefore(hoy)) {
            return Estado.VENCIDA;
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
