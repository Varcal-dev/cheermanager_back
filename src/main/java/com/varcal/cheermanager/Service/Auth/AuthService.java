package com.varcal.cheermanager.Service.Auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.security.JwtUtil;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    /** Hash valido de BCrypt para igualar tiempos cuando el email no existe (anti-enumeracion). */
    private static final String DUMMY_HASH = BCrypt.hashpw("dummy-password", BCrypt.gensalt());
    private static final int MIN_PASSWORD_LENGTH = 8;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordResetNotifier passwordResetNotifier;

    @Value("${auth.max-login-attempts}")
    private int maxLoginAttempts;

    @Value("${auth.lock-duration-minutes}")
    private int lockDurationMinutes;

    @Value("${auth.reset-password-token-expiration-minutes}")
    private int resetPasswordTokenExpirationMinutes;

    /**
     * Codigos de error: "invalid_credentials" (email inexistente O clave incorrecta, indistinguibles),
     * "account_locked" (solo si la clave era correcta pero la cuenta esta bloqueada) e
     * "inactive". Asi no se puede averiguar que correos existen.
     */
    @Transactional
    public LoginResult authenticate(String email, String password) {
        if (email == null || password == null) {
            return LoginResult.fail("invalid_credentials");
        }
        Optional<Usuario> opt = userRepository.findByEmail(email.trim());
        if (opt.isEmpty()) {
            BCrypt.checkpw(password, DUMMY_HASH); // consume tiempo similar
            return LoginResult.fail("invalid_credentials");
        }
        Usuario user = opt.get();

        boolean passwordOk;
        try {
            passwordOk = BCrypt.checkpw(password, user.getPasswordHash());
        } catch (IllegalArgumentException e) {
            return LoginResult.fail("invalid_credentials");
        }

        // Bloqueo con expiracion: si ya paso bloqueadoHasta, se libera solo.
        boolean bloqueado = Boolean.TRUE.equals(user.getBloqueado());
        if (bloqueado && user.getBloqueadoHasta() != null
                && LocalDateTime.now().isAfter(user.getBloqueadoHasta())) {
            user.setBloqueado(false);
            user.setBloqueadoHasta(null);
            user.setIntentosFallidos(0);
            bloqueado = false;
        }

        if (!passwordOk) {
            // Mientras esta bloqueada no se acumulan intentos ni se revela el estado.
            if (!bloqueado) {
                int intentos = user.getIntentosFallidos() + 1;
                user.setIntentosFallidos(intentos);
                if (intentos >= maxLoginAttempts) {
                    user.setBloqueado(true);
                    user.setBloqueadoHasta(LocalDateTime.now().plusMinutes(lockDurationMinutes));
                }
                userRepository.save(user);
            }
            return LoginResult.fail("invalid_credentials");
        }

        if (bloqueado) {
            return LoginResult.fail("account_locked");
        }

        user.setIntentosFallidos(0);
        user.setUltimoAcceso(LocalDateTime.now());
        userRepository.save(user);

        boolean requiereCambio = Boolean.TRUE.equals(user.getRequiereCambioPassword());
        return LoginResult.ok(emitirToken(user), requiereCambio);
    }

    /** Login rechazado porque el correo no pertenece a ningun club activo: mismo tiempo y mismo error. */
    public LoginResult credencialesInvalidasSinClub(String password) {
        if (password != null) {
            BCrypt.checkpw(password, DUMMY_HASH);
        }
        return LoginResult.fail("invalid_credentials");
    }

    /** Emite un JWT con los permisos ACTUALES del usuario (login y refresh). */
    @Transactional(readOnly = true)
    public String emitirToken(Usuario user) {
        Set<String> permisos = usuarioService.obtenerPermisos(user.getId());
        List<String> permisosList = new ArrayList<>(permisos);

        String rolNombre = "";
        if (user.getRol() != null) {
            rolNombre = user.getRol().getNombre();
        } else if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            rolNombre = user.getRoles().stream().map(Rol::getNombre).collect(Collectors.joining(", "));
        }
        return jwtUtil.generateToken(user.getId().longValue(), user.getEmail(), rolNombre, permisosList, user.getClubId());
    }

    /** Refresh: solo si el usuario sigue existiendo/activo (el @SQLRestriction lo garantiza). */
    @Transactional(readOnly = true)
    public Optional<String> refrescarToken(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> !Boolean.TRUE.equals(u.getBloqueado()))
                .map(this::emitirToken);
    }

    /**
     * Solicitud de recuperacion. SIEMPRE se comporta igual hacia afuera (no revela si el correo existe)
     * y NUNCA devuelve el token: lo entrega el notifier (correo / log del servidor).
     */
    @Transactional
    public void forgotPassword(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        userRepository.findByEmail(email.trim()).ifPresent(user -> {
            String token = nuevoToken();
            user.setTokenResetPassword(sha256(token)); // se guarda solo el hash
            user.setTokenResetPasswordExpiracion(LocalDateTime.now().plusMinutes(resetPasswordTokenExpirationMinutes));
            userRepository.save(user);
            passwordResetNotifier.enviar(user.getEmail(), user.getUsername(), token);
        });
    }

    @Transactional
    public PasswordResetResult resetPassword(String token, String newPassword) {
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            return new PasswordResetResult(false, "weak_password");
        }
        return userRepository.findByTokenResetPassword(sha256(token)).map(user -> {
            if (user.getTokenResetPasswordExpiracion() == null
                    || LocalDateTime.now().isAfter(user.getTokenResetPasswordExpiracion())) {
                user.setTokenResetPassword(null);
                user.setTokenResetPasswordExpiracion(null);
                userRepository.save(user);
                return new PasswordResetResult(false, "token_invalid");
            }
            user.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            user.setTokenResetPassword(null); // un solo uso
            user.setTokenResetPasswordExpiracion(null);
            user.setBloqueado(false);
            user.setBloqueadoHasta(null);
            user.setIntentosFallidos(0);
            user.setRequiereCambioPassword(false);
            userRepository.save(user);
            return new PasswordResetResult(true, null);
        }).orElse(new PasswordResetResult(false, "token_invalid")); // inexistente = expirado (mismo mensaje)
    }

    @Transactional
    public PasswordChangeResult changePassword(Long userId, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            return new PasswordChangeResult(false, "weak_password");
        }
        return userRepository.findById(userId).map(user -> {
            try {
                if (!BCrypt.checkpw(currentPassword, user.getPasswordHash())) {
                    return new PasswordChangeResult(false, "current_password_incorrect");
                }
                user.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
                user.setRequiereCambioPassword(false);
                userRepository.save(user);
                return new PasswordChangeResult(true, null);
            } catch (IllegalArgumentException e) {
                return new PasswordChangeResult(false, "invalid_hash");
            }
        }).orElse(new PasswordChangeResult(false, "user_not_found"));
    }

    public boolean tienePermiso(int userId, String permisoRequerido) {
        return usuarioService.obtenerPermisos(userId).contains(permisoRequerido);
    }

    public Set<String> getPermisosUsuario(int userId) {
        return usuarioService.obtenerPermisos(userId);
    }

    private static String nuevoToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- resultados ----

    public record LoginResult(boolean success, String token, String errorCode, boolean requiereCambioPassword) {
        static LoginResult ok(String token, boolean requiereCambio) {
            return new LoginResult(true, token, null, requiereCambio);
        }
        static LoginResult fail(String code) {
            return new LoginResult(false, null, code, false);
        }
        // getters estilo bean, por compatibilidad con el codigo existente
        public boolean isSuccess() { return success; }
        public String getToken() { return token; }
        public String getErrorCode() { return errorCode; }
        public boolean isRequiereCambioPassword() { return requiereCambioPassword; }
    }

    public record PasswordResetResult(boolean success, String errorCode) {
        public boolean isSuccess() { return success; }
        public String getErrorCode() { return errorCode; }
    }

    public record PasswordChangeResult(boolean success, String errorCode) {
        public boolean isSuccess() { return success; }
        public String getErrorCode() { return errorCode; }
    }
}