package com.varcal.cheermanager.Service.Persona;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.Models.Historiales.HistorialMedico;
import com.varcal.cheermanager.Models.Legal.Anonimizacion;
import com.varcal.cheermanager.Models.Personas.Acudiente;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.DeportistaAcudiente;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
import com.varcal.cheermanager.repository.Financiero.PagoRepository;
import com.varcal.cheermanager.repository.Historiales.HistorialMedicoRepository;
import com.varcal.cheermanager.repository.Legal.AnonimizacionRepository;
import com.varcal.cheermanager.repository.Personas.AcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaAcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;

/**
 * L7: supresion de datos personales de un titular SIN romper la contabilidad (Ley 1581, art. 8 literal e).
 * IRREVERSIBLE. Se reemplazan los datos que identifican a la persona por valores neutros, se borra su historial
 * medico y se desactiva su acceso; las facturas, pagos, asistencias y evaluaciones se CONSERVAN (ya no apuntan a
 * una persona identificable: el nombre ahora dice "Titular anonimizado"). Las facturas y pagos se guardan por
 * deber legal contable (confirmar el plazo con el contador).
 *
 * Reglas: solo deportistas en estado "Retirado"; no se permite si hay facturas con saldo pendiente; no se puede
 * anonimizar a uno mismo; exige motivo y la palabra de confirmacion. Los acudientes vinculados SOLO a este
 * deportista tambien se anonimizan (los que tienen otro deportista a cargo se dejan intactos).
 *
 * Fuera de alcance (tratar aparte): textos libres (observaciones, notificaciones), registros de acceso y de
 * auditoria, fotos ya subidas al disco y copias de seguridad existentes (se renuevan con su rotacion).
 */
@Service
public class AnonimizacionService {

    public static final String PALABRA_CONFIRMACION = "ANONIMIZAR";
    static final String NOMBRE_ANONIMO = "Titular anonimizado";

    public record Solicitud(String motivo, String confirmacion) {
    }

    public record Resultado(Integer personaId, int registrosMedicosEliminados, int acudientesAnonimizados,
            boolean accesoDesactivado) {
        public Integer getPersonaId() { return personaId; }
        public int getRegistrosMedicosEliminados() { return registrosMedicosEliminados; }
        public int getAcudientesAnonimizados() { return acudientesAnonimizados; }
        public boolean isAccesoDesactivado() { return accesoDesactivado; }
    }

    private final PersonaRepository personaRepository;
    private final DeportistaRepository deportistaRepository;
    private final UserRepository userRepository;
    private final AcudienteRepository acudienteRepository;
    private final DeportistaAcudienteRepository vinculoRepository;
    private final HistorialMedicoRepository historialRepository;
    private final FacturaRepository facturaRepository;
    private final PagoRepository pagoRepository;
    private final AnonimizacionRepository anonimizacionRepository;

    public AnonimizacionService(PersonaRepository personaRepository, DeportistaRepository deportistaRepository,
            UserRepository userRepository, AcudienteRepository acudienteRepository,
            DeportistaAcudienteRepository vinculoRepository, HistorialMedicoRepository historialRepository,
            FacturaRepository facturaRepository, PagoRepository pagoRepository,
            AnonimizacionRepository anonimizacionRepository) {
        this.personaRepository = personaRepository;
        this.deportistaRepository = deportistaRepository;
        this.userRepository = userRepository;
        this.acudienteRepository = acudienteRepository;
        this.vinculoRepository = vinculoRepository;
        this.historialRepository = historialRepository;
        this.facturaRepository = facturaRepository;
        this.pagoRepository = pagoRepository;
        this.anonimizacionRepository = anonimizacionRepository;
    }

    @Transactional
    public Resultado anonimizar(Integer personaId, Solicitud req, Integer usuarioEjecutorId) {
        if (req == null || req.motivo() == null || req.motivo().trim().length() < 5) {
            throw new IllegalArgumentException("Indica el motivo (mínimo 5 caracteres), por ejemplo el número de la solicitud del titular.");
        }
        if (!PALABRA_CONFIRMACION.equals(req.confirmacion() == null ? null : req.confirmacion().trim())) {
            throw new IllegalArgumentException("Para confirmar escribe " + PALABRA_CONFIRMACION
                    + ". Esta acción es irreversible.");
        }
        Persona p = personaRepository.findById(personaId)
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada"));
        if (NOMBRE_ANONIMO.equals(p.getNombre())) {
            throw new IllegalStateException("Esta persona ya fue anonimizada.");
        }
        Usuario ejecutor = userRepository.findById(usuarioEjecutorId.longValue()).orElse(null);
        if (ejecutor != null && ejecutor.getPersona() != null && ejecutor.getPersona().getId().equals(personaId)) {
            throw new IllegalStateException("No puedes anonimizar tu propia persona.");
        }
        Deportista d = deportistaRepository.findByPersonaId(personaId).orElseThrow(() -> new IllegalStateException(
                "Solo se anonimizan deportistas. Para otras personas, consulta con el soporte."));
        if (d.getEstado() == null || !"Retirado".equals(d.getEstado().getEstado())) {
            throw new IllegalStateException("Primero retira al deportista: solo se anonimizan deportistas en estado Retirado.");
        }
        exigirSinSaldoPendiente(personaId);

        // 1) Persona
        p.setNombre(NOMBRE_ANONIMO);
        p.setApellidos("#" + p.getId());
        p.setNumeroDocumento("ANON-" + p.getId());
        p.setDireccion(null);
        p.setTelefono(null);
        if (p.getFechaNacimiento() != null) {
            p.setFechaNacimiento(LocalDate.of(p.getFechaNacimiento().getYear(), 1, 1)); // se conserva solo el año
        }
        personaRepository.save(p);

        // 2) Ficha deportiva
        d.setContactoEmergencia(null);
        d.setFotoPerfil(null);
        d.setPeso(null);
        d.setAltura(null);
        deportistaRepository.save(d);

        // 3) Historial medico (dato sensible): se borra
        List<HistorialMedico> medico = historialRepository.findByPersonaId(personaId, Pageable.unpaged()).getContent();
        int eliminados = medico.size();
        if (eliminados > 0) {
            historialRepository.deleteAll(medico);
        }

        // 4) Acceso (usuario del deportista)
        boolean acceso = userRepository.findByPersona(p).map(u -> {
            desactivar(u);
            userRepository.save(u);
            return true;
        }).orElse(false);

        // 5) Acudientes vinculados solo a este deportista
        int acudientes = 0;
        for (DeportistaAcudiente v : vinculoRepository.findByDeportistaId(d.getId())) {
            Acudiente a = v.getAcudiente();
            List<Deportista> aCargo = vinculoRepository.deportistasDeAcudiente(a.getId());
            boolean soloEste = aCargo.stream().allMatch(x -> x.getId().equals(d.getId()));
            if (soloEste && !"Acudiente anonimizado".equals(a.getNombre())) {
                a.setNombre("Acudiente anonimizado");
                a.setApellidos("#" + a.getId());
                a.setNumeroDocumento("ANON-A" + a.getId());
                a.setTelefono(null);
                a.setEmail(null);
                a.setDireccion(null);
                acudienteRepository.save(a);
                if (a.getUsuarioId() != null) {
                    userRepository.findById(a.getUsuarioId().longValue()).ifPresent(u -> {
                        desactivar(u);
                        userRepository.save(u);
                    });
                }
                acudientes++;
            }
        }

        // 6) Constancia (sin datos personales)
        Anonimizacion c = new Anonimizacion();
        c.setPersonaId(personaId);
        c.setUsuarioId(usuarioEjecutorId);
        c.setFecha(LocalDateTime.now());
        String motivo = req.motivo().trim();
        c.setMotivo(motivo.length() > 300 ? motivo.substring(0, 300) : motivo);
        anonimizacionRepository.save(c);

        return new Resultado(personaId, eliminados, acudientes, acceso);
    }

    /** Sin deuda: el club necesita el nombre para cobrar. Factura anulada no cuenta. */
    private void exigirSinSaldoPendiente(Integer personaId) {
        List<Factura> vigentes = facturaRepository.findByPersonaIdOrderByFechaEmisionDesc(personaId).stream()
                .filter(f -> !Boolean.TRUE.equals(f.getAnulada())).toList();
        if (vigentes.isEmpty()) {
            return;
        }
        Map<Integer, BigDecimal> pagado = pagoRepository.pagadoPorFacturas(vigentes.stream().map(Factura::getId).toList());
        long conSaldo = vigentes.stream()
                .filter(f -> f.getTotal().subtract(pagado.getOrDefault(f.getId(), BigDecimal.ZERO)).signum() > 0).count();
        if (conSaldo > 0) {
            throw new IllegalStateException("No se puede anonimizar: tiene " + conSaldo
                    + " factura(s) con saldo pendiente. Cóbralas o anúlalas primero.");
        }
    }

    private static void desactivar(Usuario u) {
        u.setActivo(false);
        u.setBloqueado(true);
        u.setEmail("anonimizado-" + u.getId() + "-" + System.nanoTime() + "@anonimizado.invalid");
        u.setUsername("anonimizado-" + u.getId());
        u.setPasswordHash("!"); // no es un hash valido: nadie puede iniciar sesion
        u.setTokenResetPassword(null);
        u.setTokenResetPasswordExpiracion(null);
    }
}
