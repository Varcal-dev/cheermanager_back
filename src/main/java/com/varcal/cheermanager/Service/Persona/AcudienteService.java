package com.varcal.cheermanager.Service.Persona;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Personas.Acudiente;
import com.varcal.cheermanager.Service.Clubes.RolesPlantilla;
import com.varcal.cheermanager.Utils.ClaveTemporal;
import com.varcal.cheermanager.repository.Auth.RolRepository;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.tenant.TenantLookupRepository;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.DeportistaAcudiente;
import com.varcal.cheermanager.repository.Consentimientos.ConsentimientoRepository;
import com.varcal.cheermanager.repository.Personas.AcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaAcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

/** Acudientes de los deportistas menores de edad. Todo se filtra por club (@TenantId). */
@Service
public class AcudienteService {

    public record AcudienteRequest(String nombre, String apellidos, String tipoDocumento, String numeroDocumento,
            String telefono, String email, String direccion) {
    }

    public record VinculoRequest(Integer acudienteId, String parentesco, Boolean principal, Boolean autorizaRecoger) {
    }

    public record VinculoResponse(Integer vinculoId, Integer acudienteId, String nombre, String apellidos,
            String telefono, String email, String parentesco, boolean principal, boolean autorizaRecoger,
            boolean tieneAcceso) {
        static VinculoResponse de(DeportistaAcudiente v) {
            Acudiente a = v.getAcudiente();
            return new VinculoResponse(v.getId(), a.getId(), a.getNombre(), a.getApellidos(), a.getTelefono(),
                    a.getEmail(), v.getParentesco(), Boolean.TRUE.equals(v.getPrincipal()),
                    Boolean.TRUE.equals(v.getAutorizaRecoger()), a.getUsuarioId() != null);
        }
    }

    private final AcudienteRepository acudienteRepository;
    private final DeportistaAcudienteRepository vinculoRepository;
    private final DeportistaRepository deportistaRepository;
    private final ConsentimientoRepository consentimientoRepository;
    private final UserRepository userRepository;
    private final RolRepository rolRepository;
    private final TenantLookupRepository tenantLookup;

    public record AccesoAcudiente(String username, String claveTemporal) {
    }

    public AcudienteService(AcudienteRepository acudienteRepository, DeportistaAcudienteRepository vinculoRepository,
            DeportistaRepository deportistaRepository, ConsentimientoRepository consentimientoRepository,
            UserRepository userRepository, RolRepository rolRepository, TenantLookupRepository tenantLookup) {
        this.userRepository = userRepository;
        this.rolRepository = rolRepository;
        this.tenantLookup = tenantLookup;
        this.acudienteRepository = acudienteRepository;
        this.vinculoRepository = vinculoRepository;
        this.deportistaRepository = deportistaRepository;
        this.consentimientoRepository = consentimientoRepository;
    }

    public List<Acudiente> listar() {
        return acudienteRepository.findAll();
    }

    public Acudiente obtener(Integer id) {
        return acudienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Acudiente no encontrado con ID: " + id));
    }

    @Transactional
    public Acudiente crear(AcudienteRequest req) {
        validar(req);
        String doc = limpio(req.numeroDocumento());
        if (doc != null && acudienteRepository.findByNumeroDocumento(doc).isPresent()) {
            throw new RuntimeException("Ya existe un acudiente con ese numero de documento");
        }
        Acudiente a = new Acudiente();
        aplicar(a, req);
        return acudienteRepository.save(a);
    }

    @Transactional
    public Acudiente actualizar(Integer id, AcudienteRequest req) {
        validar(req);
        Acudiente a = obtener(id);
        String doc = limpio(req.numeroDocumento());
        if (doc != null) {
            acudienteRepository.findByNumeroDocumento(doc).ifPresent(otro -> {
                if (!otro.getId().equals(id)) {
                    throw new RuntimeException("Ya existe otro acudiente con ese numero de documento");
                }
            });
        }
        aplicar(a, req);
        return acudienteRepository.save(a);
    }

    /** No se borra un acudiente que firmo consentimientos: son evidencia legal. */
    @Transactional
    public void eliminar(Integer id) {
        Acudiente a = obtener(id);
        if (consentimientoRepository.existsByAcudienteId(a.getId())) {
            throw new RuntimeException("No se puede eliminar: el acudiente firmo consentimientos que se deben conservar");
        }
        vinculoRepository.deleteByAcudienteId(id);
        Integer usuarioId = a.getUsuarioId();
        acudienteRepository.delete(a);
        if (usuarioId != null) { // el acudiente ya no existe: su acceso al portal se desactiva
            userRepository.findById(usuarioId.longValue()).ifPresent(userRepository::delete);
        }
    }

    /**
     * Crea el usuario del portal del acudiente (rol "Acudiente", solo lectura). El usuario es su correo; la clave
     * temporal se devuelve UNA vez y el sistema le exige cambiarla al ingresar.
     */
    @Transactional
    public AccesoAcudiente crearAcceso(Integer id) {
        Acudiente a = obtener(id);
        if (a.getUsuarioId() != null) {
            throw new RuntimeException("El acudiente ya tiene acceso al portal; usa 'restablecer clave' si la olvido");
        }
        String email = limpio(a.getEmail());
        if (email == null) {
            throw new RuntimeException("El acudiente necesita un correo electronico para crear su acceso");
        }
        if (vinculoRepository.countByAcudienteId(id) == 0) {
            throw new RuntimeException("Vincula primero al acudiente con al menos un deportista");
        }
        if (tenantLookup.usernameExisteEnAlgunClub(email) || tenantLookup.emailExisteEnAlgunClub(email)) {
            throw new RuntimeException("Ese correo ya esta en uso por otro usuario");
        }
        Rol rol = rolRepository.findByNombre(RolesPlantilla.ACUDIENTE)
                .orElseThrow(() -> new RuntimeException("El club no tiene el rol 'Acudiente'"));
        String clave = ClaveTemporal.generar();
        Usuario u = new Usuario();
        u.setUsername(email);
        u.setEmail(email);
        u.setPasswordHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(clave));
        u.setRol(rol);
        u.setActivo(true);
        u.setRequiereCambioPassword(true);
        u = userRepository.save(u);
        a.setUsuarioId(u.getId());
        acudienteRepository.save(a);
        return new AccesoAcudiente(email, clave);
    }

    public List<VinculoResponse> listarDeDeportista(Integer deportistaId) {
        exigirDeportista(deportistaId);
        return vinculoRepository.findByDeportistaId(deportistaId).stream().map(VinculoResponse::de).toList();
    }

    @Transactional
    public VinculoResponse vincular(Integer deportistaId, VinculoRequest req) {
        Deportista d = exigirDeportista(deportistaId);
        if (req == null || req.acudienteId() == null) {
            throw new RuntimeException("acudienteId es obligatorio");
        }
        Acudiente a = obtener(req.acudienteId());
        if (vinculoRepository.existsByDeportistaIdAndAcudienteId(deportistaId, a.getId())) {
            throw new RuntimeException("Ese acudiente ya esta vinculado al deportista");
        }
        List<DeportistaAcudiente> actuales = vinculoRepository.findByDeportistaId(deportistaId);
        boolean principal = Boolean.TRUE.equals(req.principal()) || actuales.isEmpty(); // el primero es principal
        if (principal) {
            actuales.forEach(v -> v.setPrincipal(false)); // solo uno principal por deportista
            vinculoRepository.saveAll(actuales);
        }
        DeportistaAcudiente v = new DeportistaAcudiente();
        v.setDeportista(d);
        v.setAcudiente(a);
        v.setParentesco(limpio(req.parentesco()));
        v.setPrincipal(principal);
        v.setAutorizaRecoger(Boolean.TRUE.equals(req.autorizaRecoger()));
        return VinculoResponse.de(vinculoRepository.save(v));
    }

    @Transactional
    public void desvincular(Integer vinculoId) {
        DeportistaAcudiente v = vinculoRepository.findById(vinculoId)
                .orElseThrow(() -> new RuntimeException("Vinculo no encontrado con ID: " + vinculoId));
        vinculoRepository.delete(v);
    }

    private Deportista exigirDeportista(Integer id) {
        return deportistaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado con ID: " + id));
    }

    private void validar(AcudienteRequest r) {
        if (r == null || limpio(r.nombre()) == null || limpio(r.apellidos()) == null) {
            throw new RuntimeException("Nombre y apellidos del acudiente son obligatorios");
        }
        if (limpio(r.telefono()) == null && limpio(r.email()) == null) {
            throw new RuntimeException("Indica al menos un telefono o un correo de contacto");
        }
    }

    private void aplicar(Acudiente a, AcudienteRequest r) {
        a.setNombre(r.nombre().trim());
        a.setApellidos(r.apellidos().trim());
        a.setTipoDocumento(limpio(r.tipoDocumento()));
        a.setNumeroDocumento(limpio(r.numeroDocumento()));
        a.setTelefono(limpio(r.telefono()));
        a.setEmail(limpio(r.email()));
        a.setDireccion(limpio(r.direccion()));
    }

    private static String limpio(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}