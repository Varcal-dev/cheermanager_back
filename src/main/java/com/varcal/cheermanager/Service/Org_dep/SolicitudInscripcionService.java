package com.varcal.cheermanager.Service.Org_dep;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Org_dep.InscripcionDto;
import com.varcal.cheermanager.Models.Org_dep.Inscripcion;
import com.varcal.cheermanager.Models.Org_dep.SolicitudInscripcion;
import com.varcal.cheermanager.Models.Org_dep.SolicitudInscripcion.Estado;
import com.varcal.cheermanager.Models.Org_dep.Temporada;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Service.Financiero.FacturaService;
import com.varcal.cheermanager.repository.Org_dep.InscripcionRepository;
import com.varcal.cheermanager.repository.Org_dep.SolicitudInscripcionRepository;
import com.varcal.cheermanager.repository.Org_dep.TemporadaRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

/**
 * Renovacion de temporada UNA POR UNA: el deportista/acudiente solicita (desde el portal) y el club aprueba o
 * rechaza. La inscripcion anterior NO se cierra aqui: su membresia termina sola al vencer (fin de temporada).
 */
@Service
public class SolicitudInscripcionService {

    public record SolicitudResponse(Integer id, Integer deportistaId, String deportista, Integer temporadaId,
            String temporada, LocalDate temporadaInicio, LocalDate temporadaFin, String estado, String observaciones,
            LocalDateTime fechaSolicitud, LocalDateTime fechaResolucion, String motivoResolucion,
            Integer inscripcionId) {
        static SolicitudResponse de(SolicitudInscripcion s) {
            Deportista d = s.getDeportista();
            Temporada t = s.getTemporada();
            return new SolicitudResponse(s.getId(), d.getId(),
                    d.getPersona().getNombre() + " " + d.getPersona().getApellidos(), t.getId(), t.getNombre(),
                    t.getFechaInicio(), t.getFechaFin(), s.getEstado().name(), s.getObservaciones(),
                    s.getFechaSolicitud(), s.getFechaResolucion(), s.getMotivoResolucion(), s.getInscripcionId());
        }
    }

    public record AprobarRequest(Integer planPagoId, Integer planMensualidadId) {
    }

    private final SolicitudInscripcionRepository repo;
    private final DeportistaRepository deportistaRepository;
    private final TemporadaRepository temporadaRepository;
    private final InscripcionRepository inscripcionRepository;
    private final InscripcionService inscripcionService;

    public SolicitudInscripcionService(SolicitudInscripcionRepository repo, DeportistaRepository deportistaRepository,
            TemporadaRepository temporadaRepository, InscripcionRepository inscripcionRepository,
            InscripcionService inscripcionService) {
        this.repo = repo;
        this.deportistaRepository = deportistaRepository;
        this.temporadaRepository = temporadaRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.inscripcionService = inscripcionService;
    }

    /** Temporadas a las que aun se puede solicitar: las que no han terminado. */
    public List<Temporada> temporadasAbiertas() {
        LocalDate hoy = LocalDate.now();
        return temporadaRepository.findAllByOrderByFechaInicioDesc().stream()
                .filter(t -> !t.getFechaFin().isBefore(hoy)).toList();
    }

    /** La pertenencia del deportista al usuario la valida quien llama (PortalService). */
    @Transactional
    public SolicitudResponse crear(Integer deportistaId, Integer temporadaId, String observaciones,
            Integer usuarioId) {
        Deportista d = deportistaRepository.findById(deportistaId)
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado"));
        if (d.getEstado() != null && "Retirado".equalsIgnoreCase(d.getEstado().getEstado())) {
            throw new RuntimeException("El deportista esta retirado del club; comunicate con el club para reactivarlo");
        }
        Temporada t = temporadaRepository.findById(temporadaId)
                .orElseThrow(() -> new RuntimeException("Temporada no encontrada"));
        if (t.getFechaFin().isBefore(LocalDate.now())) {
            throw new RuntimeException("La temporada ya termino");
        }
        if (repo.existsByDeportistaIdAndTemporadaIdAndEstadoIn(deportistaId, temporadaId,
                List.of(Estado.PENDIENTE, Estado.APROBADA))) {
            throw new RuntimeException("Ya existe una solicitud pendiente o aprobada para esa temporada");
        }
        boolean yaInscrito = inscripcionRepository
                .findByDeportistaIdAndEstado(deportistaId, Inscripcion.EstadoInscripcion.Activa).stream()
                .anyMatch(i -> i.getTemporada() != null && i.getTemporada().getId().equals(temporadaId));
        if (yaInscrito) {
            throw new RuntimeException("El deportista ya esta inscrito en esa temporada");
        }

        SolicitudInscripcion s = new SolicitudInscripcion();
        s.setDeportista(d);
        s.setTemporada(t);
        s.setEstado(Estado.PENDIENTE);
        s.setObservaciones(recortar(observaciones, 300));
        s.setFechaSolicitud(LocalDateTime.now());
        s.setSolicitadaPorUsuarioId(usuarioId);
        return SolicitudResponse.de(repo.save(s));
    }

    public List<SolicitudResponse> listar(String estado) {
        List<SolicitudInscripcion> l = (estado == null || estado.isBlank())
                ? repo.findAllByOrderByFechaSolicitudDesc()
                : repo.findByEstadoOrderByFechaSolicitudDesc(parseEstado(estado));
        return l.stream().map(SolicitudResponse::de).toList();
    }

    public List<SolicitudResponse> listarDeDeportista(Integer deportistaId) {
        return repo.findByDeportistaIdOrderByFechaSolicitudDesc(deportistaId).stream()
                .map(SolicitudResponse::de).toList();
    }

    @Transactional
    public SolicitudResponse aprobar(Integer id, AprobarRequest req) {
        SolicitudInscripcion s = pendiente(id);
        if (req == null || req.planPagoId() == null) {
            throw new RuntimeException("Indica el plan de inscripcion");
        }
        Temporada t = s.getTemporada();
        InscripcionDto dto = new InscripcionDto();
        dto.setDeportistaId(s.getDeportista().getId());
        dto.setPlanPagoId(req.planPagoId());
        dto.setPlanMensualidadId(req.planMensualidadId());
        dto.setTemporadaId(t.getId());
        dto.setFechaInscripcion(LocalDate.now());
        dto.setFechaVencimiento(t.getFechaFin());
        dto.setEstado("Activa");
        Inscripcion creada = inscripcionService.crearInscripcion(dto);

        s.setEstado(Estado.APROBADA);
        s.setInscripcionId(creada.getId());
        cerrar(s, null);
        return SolicitudResponse.de(repo.save(s));
    }

    @Transactional
    public SolicitudResponse rechazar(Integer id, String motivo) {
        SolicitudInscripcion s = pendiente(id);
        s.setEstado(Estado.RECHAZADA);
        cerrar(s, FacturaService.validarMotivo(motivo));
        return SolicitudResponse.de(repo.save(s));
    }

    /** Cancelacion por quien la creo (la pertenencia la valida PortalService). */
    @Transactional
    public SolicitudResponse cancelar(Integer id) {
        SolicitudInscripcion s = pendiente(id);
        s.setEstado(Estado.CANCELADA);
        cerrar(s, "Cancelada por el solicitante");
        return SolicitudResponse.de(repo.save(s));
    }

    public SolicitudInscripcion obtener(Integer id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
    }

    private SolicitudInscripcion pendiente(Integer id) {
        SolicitudInscripcion s = obtener(id);
        if (s.getEstado() != Estado.PENDIENTE) {
            throw new RuntimeException("La solicitud ya fue resuelta (" + s.getEstado() + ")");
        }
        return s;
    }

    private void cerrar(SolicitudInscripcion s, String motivo) {
        s.setFechaResolucion(LocalDateTime.now());
        s.setResueltaPorUsuarioId(FacturaService.usuarioActualId());
        s.setMotivoResolucion(motivo);
    }

    private static Estado parseEstado(String e) {
        try {
            return Estado.valueOf(e.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Estado invalido: " + e);
        }
    }

    private static String recortar(String v, int max) {
        if (v == null || v.isBlank()) {
            return null;
        }
        String t = v.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
