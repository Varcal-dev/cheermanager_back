package com.varcal.cheermanager.Service.Legal;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Legal.SolicitudTitular;
import com.varcal.cheermanager.Models.Legal.SolicitudTitular.Estado;
import com.varcal.cheermanager.Models.Legal.SolicitudTitular.Tipo;
import com.varcal.cheermanager.repository.Legal.SolicitudTitularRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;

/** L8: registro y seguimiento de consultas y reclamos de titulares con sus plazos legales (Ley 1581). */
@Service
public class SolicitudTitularService {

    static final Set<String> CANALES = Set.of("CORREO", "PRESENCIAL", "TELEFONO", "PORTAL", "OTRO");

    public record Solicitud(Tipo tipo, String solicitanteNombre, String solicitanteDocumento,
            String solicitanteContacto, Integer personaId, String canal, String descripcion,
            LocalDate fechaRecepcion) {
    }

    public record Vista(Integer id, Tipo tipo, Estado estado, String canal, String solicitanteNombre,
            String solicitanteDocumento, String solicitanteContacto, Integer personaId, String descripcion,
            LocalDate fechaRecepcion, LocalDate fechaLimite, boolean prorrogada, String motivoProrroga,
            String respuesta, LocalDate fechaRespuesta, boolean vencida, int diasHabilesRestantes,
            Boolean respondidaEnPlazo) {
    }

    private final SolicitudTitularRepository repo;
    private final PersonaRepository personaRepository;

    public SolicitudTitularService(SolicitudTitularRepository repo, PersonaRepository personaRepository) {
        this.repo = repo;
        this.personaRepository = personaRepository;
    }

    /** Reloj, sobrescribible en pruebas. */
    protected LocalDate hoy() {
        return com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy();
    }

    static int diasPlazo(Tipo t) {
        return t == Tipo.CONSULTA ? 10 : 15;
    }

    static int diasProrroga(Tipo t) {
        return t == Tipo.CONSULTA ? 5 : 8;
    }

    @Transactional
    public Vista registrar(Solicitud s, int usuarioId) {
        if (s.tipo() == null) {
            throw new IllegalArgumentException("Indica el tipo de solicitud");
        }
        String nombre = s.solicitanteNombre() == null ? "" : s.solicitanteNombre().trim();
        if (nombre.length() < 3 || nombre.length() > 160) {
            throw new IllegalArgumentException("El nombre del solicitante es obligatorio (3 a 160 caracteres)");
        }
        String desc = s.descripcion() == null ? "" : s.descripcion().trim();
        if (desc.length() < 5 || desc.length() > 2000) {
            throw new IllegalArgumentException("Describe la solicitud (5 a 2000 caracteres)");
        }
        String canal = s.canal() == null ? "" : s.canal().trim().toUpperCase();
        if (!CANALES.contains(canal)) {
            throw new IllegalArgumentException("Canal invalido. Usa: " + String.join(", ", CANALES.stream().sorted().toList()));
        }
        LocalDate hoy = hoy();
        LocalDate recibida = s.fechaRecepcion() == null ? hoy : s.fechaRecepcion();
        if (recibida.isAfter(hoy)) {
            throw new IllegalArgumentException("La fecha de recepcion no puede ser futura");
        }
        if (s.personaId() != null && personaRepository.findById(s.personaId()).isEmpty()) {
            throw new IllegalArgumentException("La persona indicada no existe");
        }
        SolicitudTitular e = new SolicitudTitular();
        e.setTipo(s.tipo());
        e.setCanal(canal);
        e.setSolicitanteNombre(nombre);
        e.setSolicitanteDocumento(blankToNull(s.solicitanteDocumento()));
        e.setSolicitanteContacto(blankToNull(s.solicitanteContacto()));
        e.setPersonaId(s.personaId());
        e.setDescripcion(desc);
        e.setFechaRecepcion(recibida);
        e.setFechaLimite(DiasHabiles.sumar(recibida, diasPlazo(s.tipo())));
        e.setRegistradaPor(usuarioId);
        return vista(repo.save(e), hoy);
    }

    public List<Vista> listar(Estado estado) {
        LocalDate hoy = hoy();
        return repo.findAllByOrderByFechaRecepcionDescIdDesc().stream()
                .filter(e -> estado == null || e.getEstado() == estado)
                .map(e -> vista(e, hoy)).toList();
    }

    @Transactional
    public Vista ponerEnTramite(Integer id) {
        SolicitudTitular e = buscar(id);
        if (e.getEstado() != Estado.RECIBIDA) {
            throw new IllegalStateException("Solo una solicitud recibida pasa a en tramite");
        }
        e.setEstado(Estado.EN_TRAMITE);
        return vista(repo.save(e), hoy());
    }

    /** Una sola prorroga, antes de vencer el plazo y con motivo (Ley 1581: +5 dias habiles consulta, +8 reclamo). */
    @Transactional
    public Vista prorrogar(Integer id, String motivo) {
        SolicitudTitular e = buscar(id);
        if (e.getEstado() == Estado.RESPONDIDA) {
            throw new IllegalStateException("La solicitud ya fue respondida");
        }
        if (Boolean.TRUE.equals(e.getProrrogada())) {
            throw new IllegalStateException("La solicitud ya tiene una prorroga: la ley permite una sola");
        }
        if (hoy().isAfter(e.getFechaLimite())) {
            throw new IllegalStateException("El plazo ya vencio: la prorroga debe pedirse antes del vencimiento");
        }
        String m = motivo == null ? "" : motivo.trim();
        if (m.length() < 5 || m.length() > 500) {
            throw new IllegalArgumentException("Indica el motivo de la prorroga (5 a 500 caracteres)");
        }
        e.setProrrogada(true);
        e.setMotivoProrroga(m);
        e.setFechaLimite(DiasHabiles.sumar(e.getFechaLimite(), diasProrroga(e.getTipo())));
        return vista(repo.save(e), hoy());
    }

    @Transactional
    public Vista responder(Integer id, String respuesta, int usuarioId) {
        SolicitudTitular e = buscar(id);
        if (e.getEstado() == Estado.RESPONDIDA) {
            throw new IllegalStateException("La solicitud ya fue respondida");
        }
        String r = respuesta == null ? "" : respuesta.trim();
        if (r.length() < 5 || r.length() > 2000) {
            throw new IllegalArgumentException("Escribe la respuesta dada al titular (5 a 2000 caracteres)");
        }
        e.setRespuesta(r);
        e.setFechaRespuesta(hoy());
        e.setRespondidaPor(usuarioId);
        e.setEstado(Estado.RESPONDIDA);
        return vista(repo.save(e), hoy());
    }

    private SolicitudTitular buscar(Integer id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static Vista vista(SolicitudTitular e, LocalDate hoy) {
        boolean respondida = e.getEstado() == Estado.RESPONDIDA;
        boolean vencida = !respondida && hoy.isAfter(e.getFechaLimite());
        Boolean enPlazo = respondida ? !e.getFechaRespuesta().isAfter(e.getFechaLimite()) : null;
        return new Vista(e.getId(), e.getTipo(), e.getEstado(), e.getCanal(), e.getSolicitanteNombre(),
                e.getSolicitanteDocumento(), e.getSolicitanteContacto(), e.getPersonaId(), e.getDescripcion(),
                e.getFechaRecepcion(), e.getFechaLimite(), Boolean.TRUE.equals(e.getProrrogada()),
                e.getMotivoProrroga(), e.getRespuesta(), e.getFechaRespuesta(), vencida,
                respondida ? 0 : DiasHabiles.restantes(hoy, e.getFechaLimite()), enPlazo);
    }
}
