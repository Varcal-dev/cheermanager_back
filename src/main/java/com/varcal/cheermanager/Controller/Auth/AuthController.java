package com.varcal.cheermanager.Controller.Auth;

import com.varcal.cheermanager.Service.Auth.AuthService;
import com.varcal.cheermanager.security.JwtUtil;
import com.varcal.cheermanager.security.TokenBlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        AuthService.LoginResult result = authService.authenticate(loginRequest.getEmail(), loginRequest.getPassword());

        if (!result.isSuccess()) {
            String code = result.getErrorCode();
            switch (code) {
                case "account_locked":
                    return ResponseEntity.status(423).body(Map.of("success", false, "message", "Cuenta bloqueada por múltiples intentos fallidos"));
                case "invalid_password":
                    return ResponseEntity.status(401).body(Map.of("success", false, "message", "Contraseña incorrecta"));
                case "email_not_found":
                    return ResponseEntity.status(404).body(Map.of("success", false, "message", "El correo proporcionado no está registrado"));
                case "invalid_hash":
                    return ResponseEntity.status(500).body(Map.of("success", false, "message", "Error en el formato del hash de la contraseña"));
                default:
                    return ResponseEntity.status(500).body(Map.of("success", false, "message", "Error desconocido"));
            }
        }

        return ResponseEntity.ok(Map.of("success", true, "token", result.getToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Token no proporcionado"));
        }

        String token = authorizationHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", "Token inválido o expirado"));
        }

        Date expiration;
        try {
            expiration = jwtUtil.getClaims(token).getExpiration();
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", "No se pudo obtener expiración del token"));
        }
        tokenBlacklistService.blacklistToken(token, expiration);

        return ResponseEntity.ok(Map.of("success", true, "message", "Logout exitoso"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Token no proporcionado"));
        }

        String token = authorizationHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Token inválido o expirado"));
        }

        try {
            Long userId = jwtUtil.getUserId(token);
            String email = jwtUtil.getUserEmail(token);
            String rol = jwtUtil.getClaims(token).get("roles", String.class);
            java.util.List<String> permisos = jwtUtil.getPermisos(token);

            String newToken = jwtUtil.generateToken(userId, email, rol != null ? rol : "", permisos);

            return ResponseEntity.ok(Map.of("success", true, "token", newToken));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Error al generar nuevo token"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email es requerido"));
        }

        AuthService.PasswordResetResult result = authService.forgotPassword(request.getEmail());

        if (!result.isSuccess()) {
            if ("email_not_found".equals(result.getErrorCode())) {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", "El email proporcionado no está registrado"));
            }
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Error al procesar la solicitud"));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Token de recuperación enviado",
                "resetToken", result.getResetToken()
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        if (request.getToken() == null || request.getToken().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Token es requerido"));
        }
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Nueva contraseña es requerida"));
        }

        AuthService.PasswordResetResult result = authService.resetPassword(request.getToken(), request.getNewPassword());

        if (!result.isSuccess()) {
            String code = result.getErrorCode();
            if ("token_expired".equals(code)) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Token de recuperación expirado"));
            } else if ("token_not_found".equals(code)) {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", "Token de recuperación inválido"));
            }
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Error al resetear contraseña"));
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "Contraseña resetada correctamente"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody ChangePasswordRequest request) {

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Token no proporcionado"));
        }

        String token = authorizationHeader.substring(7);

        if (!jwtUtil.isTokenValid(token)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Token inválido o expirado"));
        }

        if (request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Contraseña actual es requerida"));
        }
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Nueva contraseña es requerida"));
        }

        try {
            Long userId = jwtUtil.getUserId(token);
            AuthService.PasswordChangeResult result = authService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());

            if (!result.isSuccess()) {
                String code = result.getErrorCode();
                if ("current_password_incorrect".equals(code)) {
                    return ResponseEntity.status(401).body(Map.of("success", false, "message", "Contraseña actual incorrecta"));
                } else if ("user_not_found".equals(code)) {
                    return ResponseEntity.status(404).body(Map.of("success", false, "message", "Usuario no encontrado"));
                }
                return ResponseEntity.status(500).body(Map.of("success", false, "message", "Error al cambiar contraseña"));
            }

            return ResponseEntity.ok(Map.of("success", true, "message", "Contraseña cambiada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Error procesando cambio de contraseña"));
        }
    }

    // Request/Response classes
    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class ForgotPasswordRequest {
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    public static class ResetPasswordRequest {
        private String token;
        private String newPassword;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
