package com.varcal.cheermanager.Service.Clubes;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Clubes.Club;
import com.varcal.cheermanager.Service.Auth.UsuarioService;
import com.varcal.cheermanager.Service.Funcionalidad.LogAccesoService;
import com.varcal.cheermanager.repository.Auth.RolRepository;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Clubes.ClubRepository;
import com.varcal.cheermanager.security.JwtUtil;
import com.varcal.cheermanager.tenant.TenantContext;
import com.varcal.cheermanager.tenant.TenantLookupRepository;
import com.varcal.cheermanager.Utils.ClaveTemporal;

/**
 * Herramientas del superadmin de la plataforma para trabajar DENTRO de un club: ver sus usuarios y roles, crear
 * usuarios, generar claves temporales y entrar como su administrador (acceso de soporte).
 *
 * El superadmin vive en el club "Plataforma" (id 1) y todo repositorio filtra por club, asi que para tocar datos de
 * otro club hay que abrir una transaccion NUEVA con el tenant de ese club (mismo patron que ClubService.crearClub).
 * Por eso esta clase NO es @Transactional: cada operacion abre la suya dentro de enClub().
 */
@Service
public class PlataformaService {

    /** El acceso de soporte dura poco y no se renueva (ver AuthController.refreshToken). */
    public static final long DURACION_SOPORTE_MS = 60L * 60L * 1000L;

    /** Permisos que NUNCA recibe una sesion de soporte: aceptar terminos es una decision del club, no de la plataforma. */
    static final Set<String> PERMISOS_VETADOS_EN_SOPORTE = Set.of("aceptar_terminos");

    public record UsuarioClub(Integer id, String username, String email, String rol, Boolean activo,
            LocalDateTime ultimoAcceso, Boolean requiereCambioPassword) {
    }

    public record RolClub(Integer id, String nombre) {
    }

    public record NuevoUsuarioRequest(String username, String email, Integer rolId) {
    }

    public record UsuarioCreado(Integer id, String username, String email, String rol, String claveTemporal) {
    }

    public record ClaveGenerada(Integer usuarioId, String username, String claveTemporal) {
    }

    public record AccesoSoporte(String token, Integer clubId, String clubNombre, String usuario, String rol,
            long expiraEnMs) {
    }

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final RolRepository rolRepository;
    private final TenantLookupRepository lookup;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioService usuarioService;
    private final JwtUtil jwtUtil;
    private final LogAccesoService logAccesoService;
    private final TransactionTemplate txNueva;

    public PlataformaService(ClubRepository clubRepository, UserRepository userRepository, RolRepository rolRepository,
            TenantLookupRepository lookup, PasswordEncoder passwordEncoder, UsuarioService usuarioService,
            JwtUtil jwtUtil, LogAccesoService logAccesoService, PlatformTransactionManager txManager) {
        this.clubRepository = clubRepository;
        this.userRepository = userRepository;
        this.rolRepository = rolRepository;
        this.lookup = lookup;
        this.passwordEncoder = passwordEncoder;
        this.usuarioService = usuarioService;
        this.jwtUtil = jwtUtil;
        this.logAccesoService = logAccesoService;
        this.txNueva = new TransactionTemplate(txManager);
        this.txNueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // ---------------------------------------------------------------- consulta

    public List<UsuarioClub> listarUsuarios(Integer clubId) {
        exigirClubReal(clubId);
        return enClub(clubId, () -> userRepository.findAll().stream()
                .sorted(Comparator.comparing(Usuario::getId))
                .map(u -> new UsuarioClub(u.getId(), u.getUsername(), u.getEmail(),
                        u.getRol() != null ? u.getRol().getNombre() : null, u.getActivo(), u.getUltimoAcceso(),
                        u.getRequiereCambioPassword()))
                .toList());
    }

    /** Roles que se pueden asignar desde aqui: los operativos. Deportista y Acudiente requieren una persona vinculada. */
    public List<RolClub> listarRoles(Integer clubId) {
        exigirClubReal(clubId);
        return enClub(clubId, () -> rolRepository.findAll().stream()
                .filter(r -> !RolesPlantilla.DEPORTISTA.equals(r.getNombre()) && !RolesPlantilla.ACUDIENTE.equals(r.getNombre()))
                .sorted(Comparator.comparing(Rol::getNombre))
                .map(r -> new RolClub(r.getId(), r.getNombre()))
                .toList());
    }

    // ---------------------------------------------------------------- usuarios

    public UsuarioCreado crearUsuario(Integer clubId, NuevoUsuarioRequest req) {
        exigirClubReal(clubId);
        if (req == null || req.username() == null || req.username().isBlank() || req.email() == null
                || req.email().isBlank() || req.rolId() == null) {
            throw new RuntimeException("Usuario, correo y rol son obligatorios");
        }
        String username = req.username().trim();
        String email = req.email().trim();
        if (lookup.usernameExisteEnAlgunClub(username)) {
            throw new RuntimeException("El nombre de usuario ya esta en uso");
        }
        if (lookup.emailExisteEnAlgunClub(email)) {
            throw new RuntimeException("El correo ya esta en uso");
        }
        String clave = ClaveTemporal.generar();
        return enClub(clubId, () -> {
            // rolRepository esta filtrado por club: un rol de otro club simplemente "no existe" aqui.
            Rol rol = rolRepository.findById(req.rolId())
                    .orElseThrow(() -> new RuntimeException("El rol no existe en este club"));
            if (RolesPlantilla.DEPORTISTA.equals(rol.getNombre()) || RolesPlantilla.ACUDIENTE.equals(rol.getNombre())) {
                throw new RuntimeException("Los usuarios Deportista y Acudiente se crean desde el club, vinculados a su persona");
            }
            Usuario u = new Usuario();
            u.setUsername(username);
            u.setEmail(email);
            u.setPasswordHash(passwordEncoder.encode(clave));
            u.setRol(rol);
            u.setActivo(true);
            u.setRequiereCambioPassword(true);
            u = userRepository.save(u);
            return new UsuarioCreado(u.getId(), u.getUsername(), u.getEmail(), rol.getNombre(), clave);
        });
    }

    public ClaveGenerada claveTemporal(Integer clubId, Integer usuarioId) {
        exigirClubReal(clubId);
        String clave = ClaveTemporal.generar();
        return enClub(clubId, () -> {
            Usuario u = userRepository.findById(usuarioId.longValue())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado en este club"));
            u.setPasswordHash(passwordEncoder.encode(clave));
            u.setRequiereCambioPassword(true);
            u.setBloqueado(false);
            u.setBloqueadoHasta(null);
            u.setIntentosFallidos(0);
            u.setTokenResetPassword(null);
            u.setTokenResetPasswordExpiracion(null);
            userRepository.save(u);
            return new ClaveGenerada(u.getId(), u.getUsername(), clave);
        });
    }

    // ---------------------------------------------------------------- acceso de soporte

    /**
     * Emite un token para entrar al club como su Administrador. Queda registrado en el log de acceso DEL CLUB
     * (accion ACCESO_SOPORTE, con el correo del superadmin). El token dura una hora, no se renueva y no incluye
     * el permiso de aceptar terminos.
     */
    public AccesoSoporte accesoSoporte(Integer clubId, String superadminEmail, String ip) {
        Club club = exigirClubReal(clubId);
        if (!Boolean.TRUE.equals(club.getActivo())) {
            throw new RuntimeException("El club esta suspendido: reactivalo antes de entrar");
        }
        return enClub(clubId, () -> {
            Usuario admin = userRepository.findAll().stream()
                    .filter(u -> u.getRol() != null && RolesPlantilla.ADMIN.equals(u.getRol().getNombre()))
                    .min(Comparator.comparing(Usuario::getId))
                    .orElseThrow(() -> new RuntimeException(
                            "Este club no tiene un usuario Administrador activo. Crea uno desde la lista de usuarios del club."));

            List<String> permisos = usuarioService.obtenerPermisos(admin.getId()).stream()
                    .filter(p -> !PERMISOS_VETADOS_EN_SOPORTE.contains(p))
                    .sorted()
                    .toList();
            String token = jwtUtil.generateTokenSoporte(admin.getId().longValue(), admin.getEmail(),
                    admin.getRol().getNombre(), permisos, clubId, club.getNombre(), superadminEmail, DURACION_SOPORTE_MS);

            logAccesoService.registrarAcceso(admin, admin.getEmail(), "ACCESO_SOPORTE", ip,
                    "por superadmin " + superadminEmail);
            return new AccesoSoporte(token, clubId, club.getNombre(), admin.getUsername(), admin.getRol().getNombre(),
                    DURACION_SOPORTE_MS);
        });
    }

    // ---------------------------------------------------------------- utilidades

    private Club exigirClubReal(Integer clubId) {
        if (clubId == null || clubId == Club.PLATAFORMA_ID) {
            throw new RuntimeException("Esta accion aplica a clubes, no al club Plataforma");
        }
        return clubRepository.findById(clubId).orElseThrow(() -> new RuntimeException("Club no encontrado"));
    }

    private <T> T enClub(Integer clubId, Supplier<T> accion) {
        return TenantContext.<T>runAs(clubId, () -> txNueva.execute(status -> accion.get()));
    }
}
