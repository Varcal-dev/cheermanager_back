package com.varcal.cheermanager.Service.Consentimientos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Clubes.Club;
import com.varcal.cheermanager.Models.Consentimientos.Consentimiento;
import com.varcal.cheermanager.repository.Clubes.ClubRepository;
import com.varcal.cheermanager.tenant.TenantContext;
import com.varcal.cheermanager.Models.Consentimientos.Consentimiento.Tipo;
import com.varcal.cheermanager.Models.Personas.Acudiente;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Utils.AuthenticatedUserUtil;
import com.varcal.cheermanager.repository.Consentimientos.ConsentimientoRepository;
import com.varcal.cheermanager.repository.Personas.AcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaAcudienteRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;

/**
 * Registro de autorizaciones de tratamiento de datos (Ley 1581 de 2012). Es evidencia: solo se
 * agregan filas y se revocan (fecha_revocado); nunca se editan ni se borran.
 * NO es asesoria legal: el texto que se firma y el procedimiento deben validarse con un abogado.
 */
@Service
public class ConsentimientoService {

    private static final Set<String> CANALES = Set.of("PAPEL", "DIGITAL", "OTRO");

    public record OtorgarRequest(Integer personaId, Tipo tipo, String versionTexto, String canal,
            Integer acudienteId, String observaciones) {
    }

    public record Resumen(Integer id, Integer personaId, Tipo tipo, String versionTexto, String canal,
            Integer acudienteId, LocalDateTime fechaOtorgado, LocalDateTime fechaRevocado,
            String motivoRevocacion, boolean vigente) {
        static Resumen de(Consentimiento c) {
            return new Resumen(c.getId(), c.getTitular().getId(), c.getTipo(), c.getVersionTexto(), c.getCanal(),
                    c.getAcudiente() == null ? null : c.getAcudiente().getId(), c.getFechaOtorgado(),
                    c.getFechaRevocado(), c.getMotivoRevocacion(), c.getFechaRevocado() == null);
        }
    }

    public record Pendiente(Integer deportistaId, Integer personaId, String nombre, String numeroDocumento) {
    }

    private final ConsentimientoRepository repo;
    private final PersonaRepository personaRepository;
    private final AcudienteRepository acudienteRepository;
    private final DeportistaAcudienteRepository vinculoRepository;
    private final ClubRepository clubRepository;
    private final boolean exigirSalud;

    public ConsentimientoService(ConsentimientoRepository repo, PersonaRepository personaRepository,
            AcudienteRepository acudienteRepository, DeportistaAcudienteRepository vinculoRepository,
            ClubRepository clubRepository,
            @Value("${app.consentimientos.exigir-salud:false}") boolean exigirSalud) {
        this.repo = repo;
        this.personaRepository = personaRepository;
        this.acudienteRepository = acudienteRepository;
        this.vinculoRepository = vinculoRepository;
        this.clubRepository = clubRepository;
        this.exigirSalud = exigirSalud;
    }

    @Transactional
    public Resumen otorgar(OtorgarRequest req) {
        if (req == null || req.personaId() == null || req.tipo() == null) {
            throw new RuntimeException("personaId y tipo son obligatorios");
        }
        if (req.versionTexto() == null || req.versionTexto().isBlank()) {
            throw new RuntimeException("Indica la version del texto de autorizacion que se firmo");
        }
        String canal = req.canal() == null ? "" : req.canal().trim().toUpperCase();
        if (!CANALES.contains(canal)) {
            throw new RuntimeException("Canal invalido. Usa: PAPEL, DIGITAL u OTRO");
        }
        Persona titular = personaRepository.findById(req.personaId())
                .orElseThrow(() -> new RuntimeException("Persona no encontrada con ID: " + req.personaId()));
        if (repo.existsByTitularIdAndTipoAndFechaRevocadoIsNull(titular.getId(), req.tipo())) {
            throw new RuntimeException("Esta persona ya tiene un consentimiento vigente de ese tipo");
        }

        Acudiente acudiente = null;
        if (titular.getFechaNacimiento() == null) {
            // Sin fecha de nacimiento no se puede saber si es menor: no se adivina.
            throw new RuntimeException("Registra la fecha de nacimiento de la persona antes de registrar su consentimiento");
        }
        if (esMenor(titular.getFechaNacimiento())) {
            if (req.acudienteId() == null) {
                throw new RuntimeException("El titular es menor de edad: debe otorgar el consentimiento su acudiente");
            }
            acudiente = acudienteRepository.findById(req.acudienteId())
                    .orElseThrow(() -> new RuntimeException("Acudiente no encontrado con ID: " + req.acudienteId()));
            if (!vinculoRepository.acudienteDePersona(acudiente.getId(), titular.getId())) {
                throw new RuntimeException("Ese acudiente no esta vinculado al deportista");
            }
        }

        Consentimiento c = new Consentimiento();
        c.setTitular(titular);
        c.setAcudiente(acudiente);
        c.setTipo(req.tipo());
        c.setVersionTexto(req.versionTexto().trim());
        c.setCanal(canal);
        c.setFechaOtorgado(LocalDateTime.now());
        c.setRegistradoPorUsuarioId(AuthenticatedUserUtil.getCurrentUserId().intValue());
        c.setObservaciones(req.observaciones());
        return Resumen.de(repo.save(c));
    }

    @Transactional
    public Resumen revocar(Integer id, String motivo) {
        Consentimiento c = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Consentimiento no encontrado con ID: " + id));
        if (c.getFechaRevocado() != null) {
            throw new RuntimeException("El consentimiento ya estaba revocado");
        }
        c.setFechaRevocado(LocalDateTime.now());
        c.setMotivoRevocacion(motivo);
        return Resumen.de(repo.save(c));
    }

    public List<Resumen> listarDePersona(Integer personaId) {
        return repo.findByTitularIdOrderByFechaOtorgadoDesc(personaId).stream().map(Resumen::de).toList();
    }

    public List<Pendiente> pendientes(Tipo tipo) {
        return repo.deportistasSinConsentimiento(tipo).stream()
                .map(d -> new Pendiente(d.getId(), d.getPersona().getId(),
                        d.getPersona().getNombre() + " " + d.getPersona().getApellidos(),
                        d.getPersona().getNumeroDocumento()))
                .toList();
    }

    public boolean tieneVigente(Integer personaId, Tipo tipo) {
        return repo.existsByTitularIdAndTipoAndFechaRevocadoIsNull(personaId, tipo);
    }

    /**
     * Llamado antes de guardar datos de salud. Con app.consentimientos.exigir-salud=true bloquea el
     * registro si no hay autorizacion vigente. Por defecto false para no frenar a un club que aun
     * esta cargando sus autorizaciones: activalo cuando el reporte de pendientes este en cero.
     */
    public record Exigencia(boolean activa, int pendientes) {
    }

    /** Activa si el club la prendio (L5) o si el despliegue la fuerza con app.consentimientos.exigir-salud. */
    boolean exigenciaActiva() {
        if (exigirSalud) {
            return true;
        }
        return clubRepository.findById(TenantContext.require())
                .map(c -> Boolean.TRUE.equals(c.getExigirConsentimientoSalud())).orElse(false);
    }

    public Exigencia exigencia() {
        return new Exigencia(exigenciaActiva(), pendientes(Tipo.DATOS_SALUD).size());
    }

    /** Activar solo si no quedan deportistas sin consentimiento de salud; desactivar siempre se puede. */
    @Transactional
    public Exigencia configurarExigencia(boolean activar) {
        Club club = clubRepository.findById(TenantContext.require())
                .orElseThrow(() -> new IllegalArgumentException("Club no encontrado"));
        if (activar) {
            int faltan = pendientes(Tipo.DATOS_SALUD).size();
            if (faltan > 0) {
                throw new IllegalArgumentException("No se puede activar: " + faltan
                        + " deportista(s) aun no tienen consentimiento de salud vigente.");
            }
        }
        club.setExigirConsentimientoSalud(activar);
        clubRepository.save(club);
        return new Exigencia(exigenciaActiva(), 0);
    }

    public void exigirConsentimientoSalud(Integer personaId) {
        if (exigenciaActiva() && !tieneVigente(personaId, Tipo.DATOS_SALUD)) {
            throw new RuntimeException(
                    "No hay un consentimiento vigente para tratar datos de salud de esta persona. Registralo primero.");
        }
    }

    static boolean esMenor(LocalDate nacimiento) {
        return nacimiento.isAfter(com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().minusYears(18));
    }
}