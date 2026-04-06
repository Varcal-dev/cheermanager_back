package com.varcal.cheermanager.Service.Auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.security.JwtUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${auth.max-login-attempts}")
    private int maxLoginAttempts;

    @Value("${auth.lock-duration-minutes}")
    private int lockDurationMinutes;

    @Value("${auth.reset-password-token-expiration-minutes}")
    private int resetPasswordTokenExpirationMinutes;

    public LoginResult authenticate(String email, String password) {
        return userRepository.findByEmail(email).map(user -> {
            // Verificar si la cuenta está bloqueada
            if (user.getBloqueado()) {
                return new LoginResult(false, null, "account_locked");
            }

            try {
                boolean isAuthenticated = BCrypt.checkpw(password, user.getPasswordHash());
                if (isAuthenticated) {
                    // Reset intentos fallidos y actualizar último acceso
                    user.setIntentosFallidos(0);
                    user.setUltimoAcceso(LocalDateTime.now());
                    userRepository.save(user);

                    Rol rol = user.getRol();
                    List<String> permisos = (rol != null && rol.getPermisos() != null)
                            ? rol.getPermisos().stream().map(p -> p.getNombre()).collect(Collectors.toList())
                            : List.of();

                    String token = jwtUtil.generateToken(user.getId().longValue(), user.getEmail(),
                            rol != null ? rol.getNombre() : "", permisos);

                    return new LoginResult(true, token, null);
                } else {
                    // Incrementar intentos fallidos
                    user.setIntentosFallidos(user.getIntentosFallidos() + 1);

                    // Bloquear si se alcanzaron los intentos máximos
                    if (user.getIntentosFallidos() >= maxLoginAttempts) {
                        user.setBloqueado(true);
                        userRepository.save(user);
                        return new LoginResult(false, null, "account_locked");
                    }

                    userRepository.save(user);
                    return new LoginResult(false, null, "invalid_password");
                }
            } catch (IllegalArgumentException e) {
                return new LoginResult(false, null, "invalid_hash");
            }
        }).orElse(new LoginResult(false, null, "email_not_found"));
    }

    public PasswordResetResult forgotPassword(String email) {
        return userRepository.findByEmail(email).map(user -> {
            String resetToken = UUID.randomUUID().toString();
            LocalDateTime expirationTime = LocalDateTime.now().plusMinutes(resetPasswordTokenExpirationMinutes);

            user.setTokenResetPassword(resetToken);
            user.setTokenResetPasswordExpiracion(expirationTime);
            userRepository.save(user);

            return new PasswordResetResult(true, resetToken, null);
        }).orElse(new PasswordResetResult(false, null, "email_not_found"));
    }

    public PasswordResetResult resetPassword(String token, String newPassword) {
        return userRepository.findByTokenResetPassword(token).map(user -> {
            // Validar que el token no haya expirado
            if (user.getTokenResetPasswordExpiracion() == null || LocalDateTime.now().isAfter(user.getTokenResetPasswordExpiracion())) {
                return new PasswordResetResult(false, null, "token_expired");
            }

            // Actualizar contraseña y limpiar token
            String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            user.setPasswordHash(hashedPassword);
            user.setTokenResetPassword(null);
            user.setTokenResetPasswordExpiracion(null);
            user.setBloqueado(false);  // Desbloquear la cuenta
            user.setIntentosFallidos(0);
            userRepository.save(user);

            return new PasswordResetResult(true, null, null);
        }).orElse(new PasswordResetResult(false, null, "token_not_found"));
    }

    public PasswordChangeResult changePassword(Long userId, String currentPassword, String newPassword) {
        return userRepository.findById(userId).map(user -> {
            try {
                boolean isAuthenticated = BCrypt.checkpw(currentPassword, user.getPasswordHash());
                if (!isAuthenticated) {
                    return new PasswordChangeResult(false, "current_password_incorrect");
                }

                String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                user.setPasswordHash(hashedPassword);
                userRepository.save(user);

                return new PasswordChangeResult(true, null);
            } catch (IllegalArgumentException e) {
                return new PasswordChangeResult(false, "invalid_hash");
            }
        }).orElse(new PasswordChangeResult(false, "user_not_found"));
    }

    public boolean tienePermiso(int userId, String permisoRequerido) {
        return userRepository.findById((long) userId).map(user -> {
            return user.getRol().getPermisos().stream()
                    .anyMatch(permiso -> permiso.getNombre().equals(permisoRequerido));
        }).orElse(false);
    }

    public Set<String> getPermisosUsuario(int userId) {
        return userRepository.findById((long) userId).map(user -> {
            return user.getRol().getPermisos().stream()
                    .map(permiso -> permiso.getNombre())
                    .collect(Collectors.toSet());
        }).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    public static class LoginResult {
        private final boolean success;
        private final String token;
        private final String errorCode;

        public LoginResult(boolean success, String token, String errorCode) {
            this.success = success;
            this.token = token;
            this.errorCode = errorCode;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getToken() {
            return token;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

    public static class PasswordResetResult {
        private final boolean success;
        private final String resetToken;
        private final String errorCode;

        public PasswordResetResult(boolean success, String resetToken, String errorCode) {
            this.success = success;
            this.resetToken = resetToken;
            this.errorCode = errorCode;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getResetToken() {
            return resetToken;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

    public static class PasswordChangeResult {
        private final boolean success;
        private final String errorCode;

        public PasswordChangeResult(boolean success, String errorCode) {
            this.success = success;
            this.errorCode = errorCode;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}
