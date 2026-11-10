package com.varcal.cheermanager.Service.Persona;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.Lesion;
import com.varcal.cheermanager.Models.Personas.Lesion.Estado;
import com.varcal.cheermanager.Models.Personas.Lesion.Gravedad;
import com.varcal.cheermanager.Models.Personas.Lesion.Restriccion;
import com.varcal.cheermanager.Models.Personas.Lesion.Zona;
import com.varcal.cheermanager.Models.Personas.LesionSeguimiento;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Consentimientos.ConsentimientoService;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.repository.Personas.LesionRepository;
import com.varcal.cheermanager.repository.Personas.LesionSeguimientoRepository;

/**
 * C3: lesiones y retorno al juego. Flujo: ACTIVA -> EN_RETORNO (entrenamiento modificado) -> ALTA (sin restriccion).
 * Una lesion GRAVE solo se cierra con aval medico. Es dato de salud: respeta la exigencia de consentimiento (L5).
 */
@Service
public class LesionService {

    public record Solicitud(Zona zona, String descripcion, Gravedad gravedad, Restriccion restriccion,
            LocalDate fechaLesion, LocalDate fechaEstimadaRetorno) {
    }

    /** estado y restriccion pueden ir vacios: la nota queda sin cambiar el estado. */
    public record Seguimiento(String nota, Estado estado, Restriccion restriccion, LocalDate fechaEstimadaRetorno,
            Boolean avalMedico, String avalDetalle) {
    }

    public record Item(Integer id, Integer deportistaId, String nombre, LocalDate fechaLesion, Zona zona,
            String descripcion, Gravedad gravedad, Restriccion restriccion, Estado estado,
            LocalDate fechaEstimadaRetorno, LocalDate fechaAlta, boolean avalMedico, String avalDetalle,
            int dias, boolean retornoAtrasado) {
    }

    public record Nota(Integer id, LocalDate fecha, String nota, Estado estado, Restriccion restriccion) {
    }

    public record Detalle(Item lesion, List<Nota> seguimientos) {
    }

    public record Resumen(int sinEntrenar, int modificado, int enRetorno, int retornosAtrasados, List<Item> items) {
    }

    public record Candidato(Integer deportistaId, String nombre) {
    }

    private final LesionRepository repo;
    private final LesionSeguimientoRepository notas;
    private final DeportistaRepository deportistas;
    private final ConsentimientoService consentimientos;

    public LesionService(LesionRepository repo, LesionSeguimientoRepository notas, DeportistaRepository deportistas,
            ConsentimientoService consentimientos) {
        this.repo = repo;
        this.notas = notas;
        this.deportistas = deportistas;
        this.consentimientos = consentimientos;
    }

    protected LocalDate hoy() {
        return LocalDate.now();
    }

    protected LocalDateTime ahora() {
        return LocalDateTime.now();
    }

    protected void exigirSalud(Integer personaId) {
        consentimientos.exigirConsentimientoSalud(personaId);
    }

    @Transactional
    public Item registrar(Integer deportistaId, Solicitud s, int usuarioId) {
        Deportista dep = deportistas.findById(deportistaId)
                .orElseThrow(() -> new IllegalArgumentException("Deportista no encontrado"));
        if (s == null || s.zona() == null) throw new IllegalArgumentException("Indica la zona de la lesion");
        if (s.gravedad() == null) throw new IllegalArgumentException("Indica la gravedad");
        if (s.restriccion() == null) throw new IllegalArgumentException("Indica que puede hacer el deportista mientras tanto");
        String desc = texto(s.descripcion(), 5, 300, "La descripcion");
        LocalDate hoy = hoy();
        LocalDate fecha = s.fechaLesion() == null ? hoy : s.fechaLesion();
        if (fecha.isAfter(hoy)) throw new IllegalArgumentException("La fecha de la lesion no puede ser futura");
        if (s.fechaEstimadaRetorno() != null && s.fechaEstimadaRetorno().isBefore(fecha)) {
            throw new IllegalArgumentException("El retorno estimado no puede ser anterior a la lesion");
        }
        if (s.restriccion() == Restriccion.SIN_RESTRICCION) {
            throw new IllegalArgumentException("Una lesion activa necesita restriccion: sin entrenar o entrenamiento modificado");
        }
        if (s.gravedad() == Gravedad.GRAVE && s.restriccion() != Restriccion.SIN_ENTRENAR) {
            throw new IllegalArgumentException("Una lesion grave empieza con el deportista sin entrenar");
        }
        exigirSalud(dep.getPersona().getId());
        if (repo.existsByDeportistaIdAndZonaAndEstadoNot(deportistaId, s.zona(), Estado.ALTA)) {
            throw new IllegalStateException("Ya hay una lesion abierta en esa zona: agrega un seguimiento en vez de otra lesion");
        }
        Lesion l = new Lesion();
        l.setDeportistaId(deportistaId);
        l.setFechaLesion(fecha);
        l.setZona(s.zona());
        l.setDescripcion(desc);
        l.setGravedad(s.gravedad());
        l.setRestriccion(s.restriccion());
        l.setEstado(Estado.ACTIVA);
        l.setFechaEstimadaRetorno(s.fechaEstimadaRetorno());
        l.setRegistradoPor(usuarioId);
        l.setFechaRegistro(ahora());
        return item(repo.save(l), nombre(dep), hoy);
    }

    @Transactional
    public Item seguimiento(Integer lesionId, Seguimiento s, int usuarioId) {
        Lesion l = repo.findById(lesionId).orElseThrow(() -> new IllegalArgumentException("Lesion no encontrada"));
        if (l.getEstado() == Estado.ALTA) throw new IllegalStateException("La lesion ya tiene alta: registra una nueva si se repite");
        String nota = texto(s == null ? null : s.nota(), 3, 500, "La nota");
        Estado nuevo = s.estado() == null ? l.getEstado() : s.estado();
        Restriccion restr = s.restriccion() == null ? l.getRestriccion() : s.restriccion();
        LocalDate hoy = hoy();

        if (nuevo == Estado.EN_RETORNO && restr == Restriccion.SIN_RESTRICCION) {
            throw new IllegalArgumentException("En retorno el deportista entrena de forma modificada, no sin restriccion");
        }
        if (nuevo == Estado.ACTIVA && restr == Restriccion.SIN_RESTRICCION) {
            throw new IllegalArgumentException("Una lesion activa necesita restriccion");
        }
        if (nuevo == Estado.EN_RETORNO && l.getEstado() == Estado.ACTIVA && s.restriccion() == null) {
            restr = Restriccion.MODIFICADO; // al pasar a retorno, por defecto entrena modificado
        }
        if (nuevo == Estado.ALTA) {
            boolean aval = Boolean.TRUE.equals(s.avalMedico()) || l.isAvalMedico();
            if (l.getGravedad() == Gravedad.GRAVE && !aval) {
                throw new IllegalArgumentException("Una lesion grave solo se da de alta con aval medico");
            }
            if (Boolean.TRUE.equals(s.avalMedico())) {
                l.setAvalMedico(true);
                l.setAvalDetalle(opcional(s.avalDetalle(), 150));
            }
            restr = Restriccion.SIN_RESTRICCION;
            l.setFechaAlta(hoy);
        } else if (s.fechaEstimadaRetorno() != null) {
            if (s.fechaEstimadaRetorno().isBefore(l.getFechaLesion())) {
                throw new IllegalArgumentException("El retorno estimado no puede ser anterior a la lesion");
            }
            l.setFechaEstimadaRetorno(s.fechaEstimadaRetorno());
        }
        l.setEstado(nuevo);
        l.setRestriccion(restr);
        repo.save(l);

        LesionSeguimiento n = new LesionSeguimiento();
        n.setLesionId(l.getId());
        n.setFecha(hoy);
        n.setNota(nota);
        n.setEstado(nuevo);
        n.setRestriccion(restr);
        n.setUsuarioId(usuarioId);
        n.setFechaRegistro(ahora());
        notas.save(n);
        return item(l, nombre(deportistas.findById(l.getDeportistaId()).orElse(null)), hoy);
    }

    public Detalle detalle(Integer lesionId) {
        Lesion l = repo.findById(lesionId).orElseThrow(() -> new IllegalArgumentException("Lesion no encontrada"));
        String nombre = nombre(deportistas.findById(l.getDeportistaId()).orElse(null));
        List<Nota> ns = notas.findByLesionIdOrderByIdAsc(lesionId).stream()
                .map(n -> new Nota(n.getId(), n.getFecha(), n.getNota(), n.getEstado(), n.getRestriccion())).toList();
        return new Detalle(item(l, nombre, hoy()), ns);
    }

    /** @param vigentes true = solo ACTIVA y EN_RETORNO; false = tambien las dadas de alta. */
    public Resumen listar(boolean vigentes) {
        LocalDate hoy = hoy();
        List<Lesion> ls = vigentes ? repo.findByEstadoNotOrderByFechaLesionDescIdDesc(Estado.ALTA)
                : repo.findAllByOrderByFechaLesionDescIdDesc();
        Map<Integer, Deportista> deps = deportistas.findAll().stream()
                .collect(Collectors.toMap(Deportista::getId, Function.identity(), (a, b) -> a));
        List<Item> items = new ArrayList<>();
        int sin = 0, mod = 0, ret = 0, atras = 0;
        for (Lesion l : ls) {
            Item i = item(l, nombre(deps.get(l.getDeportistaId())), hoy);
            items.add(i);
            if (l.getEstado() != Estado.ALTA) {
                if (l.getRestriccion() == Restriccion.SIN_ENTRENAR) sin++;
                if (l.getRestriccion() == Restriccion.MODIFICADO) mod++;
                if (l.getEstado() == Estado.EN_RETORNO) ret++;
                if (i.retornoAtrasado()) atras++;
            }
        }
        return new Resumen(sin, mod, ret, atras, items);
    }

    public List<Item> historial(Integer deportistaId) {
        LocalDate hoy = hoy();
        String nombre = nombre(deportistas.findById(deportistaId).orElse(null));
        return repo.findByDeportistaIdOrderByFechaLesionDescIdDesc(deportistaId).stream().map(l -> item(l, nombre, hoy)).toList();
    }

    /**
     * Restriccion que rige hoy para un deportista (la mas fuerte entre sus lesiones abiertas). Pensado para el
     * roster de competencia (C2) y para asistencia.
     */
    public Restriccion restriccionVigente(Integer deportistaId) {
        Restriccion peor = Restriccion.SIN_RESTRICCION;
        for (Lesion l : repo.findByDeportistaIdOrderByFechaLesionDescIdDesc(deportistaId)) {
            if (l.getEstado() == Estado.ALTA) continue;
            if (l.getRestriccion() == Restriccion.SIN_ENTRENAR) return Restriccion.SIN_ENTRENAR;
            if (l.getRestriccion() == Restriccion.MODIFICADO) peor = Restriccion.MODIFICADO;
        }
        return peor;
    }

    /** Deportistas activos para elegir al registrar una lesion. */
    public List<Candidato> candidatos() {
        return deportistas.findAll().stream()
                .filter(d -> d.getEstado() != null && "Activo".equalsIgnoreCase(d.getEstado().getEstado()))
                .map(d -> new Candidato(d.getId(), nombre(d)))
                .sorted(Comparator.comparing(Candidato::nombre, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private Item item(Lesion l, String nombre, LocalDate hoy) {
        LocalDate fin = l.getFechaAlta() != null ? l.getFechaAlta() : hoy;
        boolean atrasado = l.getEstado() != Estado.ALTA && l.getFechaEstimadaRetorno() != null
                && hoy.isAfter(l.getFechaEstimadaRetorno());
        return new Item(l.getId(), l.getDeportistaId(), nombre, l.getFechaLesion(), l.getZona(), l.getDescripcion(),
                l.getGravedad(), l.getRestriccion(), l.getEstado(), l.getFechaEstimadaRetorno(), l.getFechaAlta(),
                l.isAvalMedico(), l.getAvalDetalle(), (int) ChronoUnit.DAYS.between(l.getFechaLesion(), fin), atrasado);
    }

    private static String nombre(Deportista d) {
        if (d == null || d.getPersona() == null) return "(sin nombre)";
        Persona p = d.getPersona();
        return p.getNombre() + " " + p.getApellidos();
    }

    private static String texto(String s, int min, int max, String campo) {
        String t = s == null ? "" : s.replaceAll("[\\p{Cntrl}&&[^\\n]]", "").trim();
        if (t.length() < min) throw new IllegalArgumentException(campo + " es obligatoria (minimo " + min + " caracteres)");
        if (t.length() > max) throw new IllegalArgumentException(campo + " admite maximo " + max + " caracteres");
        return t;
    }

    private static String opcional(String s, int max) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
