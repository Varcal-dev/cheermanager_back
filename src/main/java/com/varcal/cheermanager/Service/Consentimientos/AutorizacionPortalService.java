package com.varcal.cheermanager.Service.Consentimientos;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Consentimientos.Consentimiento.Tipo;
import com.varcal.cheermanager.Models.Personas.Acudiente;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Personas.AcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaAcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

/**
 * L3: el acudiente (o el deportista mayor de edad) firma desde el portal las autorizaciones de tratamiento
 * de datos (canal DIGITAL). Reutiliza ConsentimientoService.otorgar, asi se aplican las mismas reglas
 * (menor => acudiente vinculado, sin duplicar vigentes) y queda el mismo registro de evidencia.
 * Es una ayuda de gestion, no asesoria legal: el texto que se firma lo revisa un abogado.
 */
@Service
public class AutorizacionPortalService {

    /** Version del texto de autorizacion (seccion 4 del documento legal) que se muestra y se registra. */
    public static final String VERSION_TEXTO = "2026-11";

    public record TipoEstado(Tipo tipo, boolean vigente) {
    }

    public record Item(Integer deportistaId, String nombre, boolean menor, boolean puedeFirmar, String motivoNoPuede,
            String version, List<TipoEstado> tipos) {
    }

    public record Firma(boolean tratamiento, boolean salud, boolean imagen) {
    }

    private final UserRepository userRepository;
    private final DeportistaRepository deportistaRepository;
    private final AcudienteRepository acudienteRepository;
    private final DeportistaAcudienteRepository vinculoRepository;
    private final ConsentimientoService consentimientos;

    public AutorizacionPortalService(UserRepository userRepository, DeportistaRepository deportistaRepository,
            AcudienteRepository acudienteRepository, DeportistaAcudienteRepository vinculoRepository,
            ConsentimientoService consentimientos) {
        this.userRepository = userRepository;
        this.deportistaRepository = deportistaRepository;
        this.acudienteRepository = acudienteRepository;
        this.vinculoRepository = vinculoRepository;
        this.consentimientos = consentimientos;
    }

    /** Autorizaciones de los deportistas a cargo del usuario (o el propio deportista). */
    public List<Item> mias(Integer usuarioId) {
        Usuario u = usuario(usuarioId);
        Acudiente ac = acudienteRepository.findByUsuarioId(u.getId()).orElse(null);
        return misDeportistas(u, ac).stream().map(d -> item(d, u, ac)).toList();
    }

    @Transactional
    public Item firmar(Integer usuarioId, Integer deportistaId, Firma firma, String ip) {
        Usuario u = usuario(usuarioId);
        Acudiente ac = acudienteRepository.findByUsuarioId(u.getId()).orElse(null);
        // Solo un deportista del usuario; si no, el mismo error que si no existiera.
        Deportista d = misDeportistas(u, ac).stream().filter(x -> x.getId().equals(deportistaId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Deportista no encontrado"));
        Item actual = item(d, u, ac);
        if (!actual.puedeFirmar()) {
            throw new IllegalArgumentException(actual.motivoNoPuede());
        }
        Integer personaId = d.getPersona().getId();
        boolean tieneTratamiento = consentimientos.tieneVigente(personaId, Tipo.TRATAMIENTO_DATOS);
        if (firma == null || (!tieneTratamiento && !firma.tratamiento())) {
            throw new IllegalArgumentException(
                    "La autorización de tratamiento de datos personales es obligatoria para continuar.");
        }
        Integer acudienteId = actual.menor() ? ac.getId() : null;
        String obs = "Firmado en el portal" + (ip == null || ip.isBlank() ? "" : " desde IP " + ip.trim());
        if (obs.length() > 300) obs = obs.substring(0, 300);
        if (firma.tratamiento()) otorgarSiFalta(personaId, Tipo.TRATAMIENTO_DATOS, acudienteId, obs);
        if (firma.salud()) otorgarSiFalta(personaId, Tipo.DATOS_SALUD, acudienteId, obs);
        if (firma.imagen()) otorgarSiFalta(personaId, Tipo.IMAGEN, acudienteId, obs);
        return item(d, u, ac);
    }

    private void otorgarSiFalta(Integer personaId, Tipo tipo, Integer acudienteId, String obs) {
        if (!consentimientos.tieneVigente(personaId, tipo)) {
            consentimientos.otorgar(new ConsentimientoService.OtorgarRequest(personaId, tipo, VERSION_TEXTO,
                    "DIGITAL", acudienteId, obs));
        }
    }

    private Item item(Deportista d, Usuario u, Acudiente ac) {
        Persona p = d.getPersona();
        LocalDate nac = p.getFechaNacimiento();
        boolean menor = nac != null && nac.isAfter(com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().minusYears(18));
        boolean puede = true;
        String motivo = null;
        if (nac == null) {
            puede = false;
            motivo = "El club debe registrar la fecha de nacimiento del deportista antes de poder firmar.";
        } else if (menor) {
            boolean esSuAcudiente = ac != null && vinculoRepository.acudienteDePersona(ac.getId(), p.getId());
            if (!esSuAcudiente) {
                puede = false;
                motivo = "Por ser menor de edad, la autorización la debe firmar su acudiente.";
            }
        } else if (u.getPersona() == null || !u.getPersona().getId().equals(p.getId())) {
            // Mayor de edad: solo firma el mismo deportista (un acudiente no puede autorizar por un adulto).
            puede = false;
            motivo = "El deportista es mayor de edad: debe firmar él mismo desde su propia cuenta.";
        }
        List<TipoEstado> tipos = new ArrayList<>();
        for (Tipo t : Tipo.values()) {
            tipos.add(new TipoEstado(t, consentimientos.tieneVigente(p.getId(), t)));
        }
        return new Item(d.getId(), p.getNombre() + " " + p.getApellidos(), menor, puede, motivo, VERSION_TEXTO, tipos);
    }

    private Usuario usuario(Integer id) {
        return userRepository.findById(id.longValue()).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private List<Deportista> misDeportistas(Usuario u, Acudiente ac) {
        List<Deportista> res = new ArrayList<>();
        if (u.getPersona() != null) {
            deportistaRepository.findByPersonaId(u.getPersona().getId()).ifPresent(res::add);
        }
        if (ac != null) {
            vinculoRepository.deportistasDeAcudiente(ac.getId()).stream()
                    .filter(d -> res.stream().noneMatch(x -> x.getId().equals(d.getId()))).forEach(res::add);
        }
        return res;
    }
}
