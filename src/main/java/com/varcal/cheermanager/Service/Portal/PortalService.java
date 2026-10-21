package com.varcal.cheermanager.Service.Portal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Financiero.FacturaResponseDTO;
import com.varcal.cheermanager.DTO.Financiero.PagoResponseDTO;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.EvaluacionRutina;
import com.varcal.cheermanager.Models.Org_dep.DeportistaPerteneceGrupo;
import com.varcal.cheermanager.Models.Org_dep.Inscripcion;
import com.varcal.cheermanager.Models.Org_dep.Temporada;
import com.varcal.cheermanager.Models.Personas.Acudiente;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Financiero.FacturaService;
import com.varcal.cheermanager.Service.Financiero.PagoService;
import com.varcal.cheermanager.Service.Org_dep.SolicitudInscripcionService;
import com.varcal.cheermanager.Service.Org_dep.SolicitudInscripcionService.SolicitudResponse;
import com.varcal.cheermanager.Utils.AuthenticatedUserUtil;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Evaluaciones.EvaluacionRepository;
import com.varcal.cheermanager.repository.Evaluaciones.ObjetivoRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.EvaluacionRutinaRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.AsistenciaRepository;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;
import com.varcal.cheermanager.repository.Org_dep.InscripcionRepository;
import com.varcal.cheermanager.repository.Personas.AcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaAcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

/**
 * Portal de SOLO LECTURA para deportistas y acudientes. Cada consulta valida que el deportista sea del usuario
 * (el mismo, o uno de los que tiene a cargo un acudiente): el permiso acceder_portal NO basta para ver datos ajenos.
 */
@Service
@Transactional(readOnly = true)
public class PortalService {

    public record MiPerfil(String tipo, String nombre, List<DeportistaResumen> deportistas) {
    }

    public record DeportistaResumen(Integer id, String nombre, String estado, List<String> grupos,
            Integer asistenciasPresentes, Integer asistenciasTotales, Integer porcentajeAsistencia,
            List<InscripcionResumen> inscripciones) {
    }

    public record InscripcionResumen(Integer id, String temporada, String estado, LocalDate fechaInscripcion,
            LocalDate fechaVencimiento) {
    }

    public record AsistenciaItem(LocalDate fecha, String estado, String dia, String horaInicio, String horaFin) {
    }

    public record EvaluacionItem(LocalDate fecha, String criterio, String categoria, Integer puntaje) {
    }

    public record ObjetivoItem(String nombre, String descripcion, LocalDate fechaCreacion, String estado) {
    }

    public record RutinaItem(Integer id, LocalDate fecha, String grupo, String nivel, String evento,
            java.math.BigDecimal puntajeTotal, String observaciones) {
    }

    public record TemporadaItem(Integer id, String nombre, LocalDate fechaInicio, LocalDate fechaFin) {
    }

    private final UserRepository userRepository;
    private final DeportistaRepository deportistaRepository;
    private final AcudienteRepository acudienteRepository;
    private final DeportistaAcudienteRepository vinculoRepository;
    private final FacturaService facturaService;
    private final PagoService pagoService;
    private final AsistenciaRepository asistenciaRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final ObjetivoRepository objetivoRepository;
    private final EvaluacionRutinaRepository rutinaRepository;
    private final DeportistaPerteneceGrupoRepository grupoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final SolicitudInscripcionService solicitudService;

    public PortalService(UserRepository userRepository, DeportistaRepository deportistaRepository,
            AcudienteRepository acudienteRepository, DeportistaAcudienteRepository vinculoRepository,
            FacturaService facturaService, PagoService pagoService, AsistenciaRepository asistenciaRepository,
            EvaluacionRepository evaluacionRepository, ObjetivoRepository objetivoRepository,
            EvaluacionRutinaRepository rutinaRepository, DeportistaPerteneceGrupoRepository grupoRepository,
            InscripcionRepository inscripcionRepository, SolicitudInscripcionService solicitudService) {
        this.userRepository = userRepository;
        this.deportistaRepository = deportistaRepository;
        this.acudienteRepository = acudienteRepository;
        this.vinculoRepository = vinculoRepository;
        this.facturaService = facturaService;
        this.pagoService = pagoService;
        this.asistenciaRepository = asistenciaRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.objetivoRepository = objetivoRepository;
        this.rutinaRepository = rutinaRepository;
        this.grupoRepository = grupoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.solicitudService = solicitudService;
    }

    // ---------- Identidad y pertenencia ----------

    private Usuario usuario() {
        Long id = AuthenticatedUserUtil.getCurrentUserId();
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    /** Deportistas a los que este usuario tiene acceso: el propio y/o los que tiene a cargo como acudiente. */
    private List<Deportista> misDeportistas(Usuario u) {
        List<Deportista> res = new ArrayList<>();
        if (u.getPersona() != null) {
            deportistaRepository.findByPersonaId(u.getPersona().getId()).ifPresent(res::add);
        }
        acudienteRepository.findByUsuarioId(u.getId())
                .ifPresent(a -> vinculoRepository.deportistasDeAcudiente(a.getId()).stream()
                        .filter(d -> res.stream().noneMatch(x -> x.getId().equals(d.getId()))).forEach(res::add));
        return res;
    }

    /** Devuelve el deportista SOLO si es del usuario autenticado; si no, el mismo error que si no existiera. */
    private Deportista propio(Integer deportistaId) {
        return misDeportistas(usuario()).stream().filter(d -> d.getId().equals(deportistaId)).findFirst()
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado"));
    }

    public Integer usuarioActualId() {
        return usuario().getId();
    }

    // ---------- Perfil ----------

    public MiPerfil perfil() {
        Usuario u = usuario();
        Acudiente ac = acudienteRepository.findByUsuarioId(u.getId()).orElse(null);
        String nombre;
        String tipo;
        if (ac != null) {
            tipo = "ACUDIENTE";
            nombre = ac.getNombre() + " " + ac.getApellidos();
        } else {
            tipo = "DEPORTISTA";
            Persona p = u.getPersona();
            nombre = p != null ? p.getNombre() + " " + p.getApellidos() : u.getUsername();
        }
        return new MiPerfil(tipo, nombre, misDeportistas(u).stream().map(this::resumen).toList());
    }

    private DeportistaResumen resumen(Deportista d) {
        Persona p = d.getPersona();
        List<String> grupos = grupoRepository.findByDeportistaIdAndFechaFinIsNull(d.getId()).stream()
                .map(g -> g.getGrupo().getNombre()).toList();
        LocalDate hasta = LocalDate.now();
        LocalDate desde = hasta.minusDays(90);
        long total = asistenciaRepository.countByDeportistaIdAndFechaBetween(d.getId(), desde, hasta);
        long presentes = asistenciaRepository.contarPorDeportistaYEstadoEnRango(d.getId(), desde, hasta, "Presente");
        Integer pct = total == 0 ? null : (int) Math.round(presentes * 100.0 / total);
        List<InscripcionResumen> insc = inscripcionRepository.findByDeportistaId(d.getId().longValue()).stream()
                .map(i -> new InscripcionResumen(i.getId(), i.getTemporada() != null ? i.getTemporada().getNombre() : null,
                        i.getEstado() != null ? i.getEstado().name() : null, i.getFechaInscripcion(),
                        i.getFechaVencimiento()))
                .toList();
        return new DeportistaResumen(d.getId(), p.getNombre() + " " + p.getApellidos(),
                d.getEstado() != null ? d.getEstado().getEstado() : null, grupos, (int) presentes, (int) total, pct,
                insc);
    }

    // ---------- Datos del deportista (solo lectura) ----------

    public List<FacturaResponseDTO> facturas(Integer deportistaId) {
        return facturaService.listarPorPersona(propio(deportistaId).getPersona().getId());
    }

    public List<PagoResponseDTO> pagos(Integer deportistaId) {
        return pagoService.listarPorPersona(propio(deportistaId).getPersona().getId());
    }

    public List<AsistenciaItem> asistencias(Integer deportistaId, LocalDate desde, LocalDate hasta) {
        Deportista d = propio(deportistaId);
        LocalDate h = hasta != null ? hasta : LocalDate.now();
        LocalDate dd = desde != null ? desde : h.minusDays(90);
        return asistenciaRepository.findByDeportistaIdAndFechaBetweenOrderByFechaDesc(d.getId(), dd, h).stream()
                .map(a -> new AsistenciaItem(a.getFecha(),
                        a.getEstadoAsistencia() != null ? a.getEstadoAsistencia().getEstado() : null,
                        a.getHorario() != null && a.getHorario().getDia() != null ? a.getHorario().getDia().getDia()
                                : null,
                        a.getHorario() != null && a.getHorario().getHoraInicio() != null
                                ? a.getHorario().getHoraInicio().toString() : null,
                        a.getHorario() != null && a.getHorario().getHoraFin() != null
                                ? a.getHorario().getHoraFin().toString() : null))
                .toList();
    }

    public List<EvaluacionItem> evaluaciones(Integer deportistaId) {
        Deportista d = propio(deportistaId);
        return evaluacionRepository.findByDeportistaIdOrderByFechaDesc(d.getId()).stream()
                .map(e -> new EvaluacionItem(e.getFecha(), e.getCriterioEvaluacion().getNombre(),
                        e.getCriterioEvaluacion().getCategoria() != null
                                ? e.getCriterioEvaluacion().getCategoria().getCategoria() : null,
                        e.getPuntajeObtenido()))
                .toList();
    }

    public List<ObjetivoItem> objetivos(Integer deportistaId) {
        Deportista d = propio(deportistaId);
        return objetivoRepository.findByDeportistaIdOrderByFechaCreacionDesc(d.getId()).stream()
                .map(o -> new ObjetivoItem(o.getNombre(), o.getDescripcion(), o.getFechaCreacion(),
                        o.getEstadoObjetivo() != null ? o.getEstadoObjetivo().getNombreEstado() : null))
                .toList();
    }

    /** Rutinas (puntajes por equipo) de los grupos a los que el deportista pertenece HOY. */
    public List<RutinaItem> rutinas(Integer deportistaId) {
        Deportista d = propio(deportistaId);
        List<RutinaItem> res = new ArrayList<>();
        for (DeportistaPerteneceGrupo m : grupoRepository.findByDeportistaIdAndFechaFinIsNull(d.getId())) {
            for (EvaluacionRutina r : rutinaRepository.findByGrupoIdOrderByFechaDesc(m.getGrupo().getId())) {
                res.add(new RutinaItem(r.getId(), r.getFecha(), m.getGrupo().getNombre(),
                        r.getNivel() != null ? r.getNivel().getNombre() : null, r.getEvento(), r.getPuntajeTotal(),
                        r.getObservaciones()));
            }
        }
        res.sort((a, b) -> b.fecha().compareTo(a.fecha()));
        return res;
    }

    // ---------- Solicitudes de inscripcion ----------

    public List<TemporadaItem> temporadasAbiertas() {
        return solicitudService.temporadasAbiertas().stream()
                .map(t -> new TemporadaItem(t.getId(), t.getNombre(), t.getFechaInicio(), t.getFechaFin())).toList();
    }

    public List<SolicitudResponse> solicitudes(Integer deportistaId) {
        return solicitudService.listarDeDeportista(propio(deportistaId).getId());
    }

    @Transactional
    public SolicitudResponse solicitar(Integer deportistaId, Integer temporadaId, String observaciones) {
        Deportista d = propio(deportistaId);
        return solicitudService.crear(d.getId(), temporadaId, observaciones, usuario().getId());
    }

    @Transactional
    public SolicitudResponse cancelarSolicitud(Integer solicitudId) {
        var s = solicitudService.obtener(solicitudId);
        propio(s.getDeportista().getId()); // solo si es de uno de MIS deportistas
        return solicitudService.cancelar(solicitudId);
    }
}
