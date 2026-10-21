package com.varcal.cheermanager.Controller.Auth;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Service.Auth.AuthService;
import com.varcal.cheermanager.Service.Funcionalidad.LogAccesoService;
import com.varcal.cheermanager.Utils.SinPermisoEspecifico;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.security.JwtUtil;
import com.varcal.cheermanager.security.TokenBlacklistService;
import com.varcal.cheermanager.repository.Clubes.ClubRepository;
import com.varcal.cheermanager.tenant.TenantContext;
import com.varcal.cheermanager.tenant.TenantLookupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
@SinPermisoEspecifico("Endpoints de autenticacion: publicos o solo requieren sesion")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private LogAccesoService logAccesoService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantLookupRepository tenantLookup;

    @Autowired
    private ClubRepository clubRepository;

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String email = loginRequest.getEmail();

        // 1) En que club esta este correo (consulta sin filtro de club, solo devuelve el id).
        //    Si el club esta suspendido se trata igual que un correo inexistente.
        Integer clubId = tenantLookup.clubIdPorEmail(email)
                .filter(clubRepository::existsByIdAndActivoTrue).orElse(null);

        if (clubId == null) {
            authService.credencialesInvalidasSinClub(loginRequest.getPassword());
            log.warn("Login fallido (correo sin club activo) desde {}", ip);
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Correo o contrasena incorrectos"));
        }

        // 2) Todo lo demas corre con el tenant de ese club.
        return TenantContext.<ResponseEntity<?>>runAs(clubId, () -> {
            AuthService.LoginResult result = authService.authenticate(email, loginRequest.getPassword());
            Usuario usuario = userRepository.findByEmail(email.trim()).orElse(null);

            if (!result.isSuccess()) {
                String code = result.getErrorCode();
                String accionLog = "account_locked".equals(code) ? "CUENTA_BLOQUEADA" : "LOGIN_FALLIDO";
                logAccesoService.registrarAcceso(usuario, email, accionLog, ip, code);

                if ("account_locked".equals(code)) {
                    return ResponseEntity.status(423).body(Map.of("success", false,
                            "message", "Cuenta bloqueada temporalmente por multiples intentos fallidos. Intenta mas tarde."));
                }
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Correo o contrasena incorrectos"));
            }

            logAccesoService.registrarAcceso(usuario, email, "LOGIN_EXITOSO", ip, null);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "token", result.getToken(),
                    "requiereCambioPassword", result.isRequiereCambioPassword()));
        });
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            HttpServletRequest request) {
        String token = bearer(authorizationHeader);
        if (token == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Token no proporcionado"));
        }
        if (!jwtUtil.isTokenValid(token)) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message", "Token invalido o expirado"));
        }

        Date expiration = jwtUtil.getClaims(token).getExpiration();
        tokenBlacklistService.blacklistToken(token, expiration);

        try {
            Long userId = jwtUtil.getUserId(token);
            Usuario usuario = userRepository.findById(userId).orElse(null);
            String email = usuario != null ? usuario.getEmail() : jwtUtil.getUserEmail(token);
            logAccesoService.registrarAcceso(usuario, email, "LOGOUT", request.getRemoteAddr(), null);
        } catch (Exception e) {
            // El logout ya ocurrio; un fallo del log de auditoria no lo revierte.
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Logout exitoso"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        String token = bearer(authorizationHeader);
        if (token == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Token no proporcionado"));
        }
        if (tokenBlacklistService.isBlacklisted(token) || !jwtUtil.isTokenValid(token)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Token invalido o expirado"));
        }

        Long userId = jwtUtil.getUserId(token);
        // Se re-valida al usuario y se recalculan sus permisos (antes se copiaban los del token viejo).
        return authService.refrescarToken(userId)
                .map(newToken -> {
                    // El token anterior queda invalidado.
                    tokenBlacklistService.blacklistToken(token, jwtUtil.getClaims(token).getExpiration());
                    return ResponseEntity.ok(Map.of("success", true, "token", newToken));
                })
                .orElseGet(() -> ResponseEntity.status(401)
                        .body(Map.of("success", false, "message", "Usuario no disponible")));
    }

    /** Siempre responde igual, exista o no el correo, y nunca devuelve el token. */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email es requerido"));
        }
        // Se resuelve el club del correo; si no existe/esta suspendido no se hace nada (misma respuesta).
        tenantLookup.clubIdPorEmail(request.getEmail())
                .filter(clubRepository::existsByIdAndActivoTrue)
                .ifPresent(clubId -> TenantContext.runAs(clubId, () -> authService.forgotPassword(request.getEmail())));
        return ResponseEntity.ok(Map.of("success", true,
                "message", "Si el correo esta registrado, recibiras instrucciones para restablecer tu contrasena."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        if (request.getToken() == null || request.getToken().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Token es requerido"));
        }
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Nueva contrasena es requerida"));
        }

        Integer clubId = tenantLookup.clubIdPorTokenReset(AuthService.sha256(request.getToken()))
                .filter(clubRepository::existsByIdAndActivoTrue).orElse(null);
        if (clubId == null) {
            return ResponseEntity.status(400).body(Map.of("success", false,
                    "message", "El enlace de recuperacion es invalido o expiro"));
        }
        AuthService.PasswordResetResult result = TenantContext.runAs(clubId,
                () -> authService.resetPassword(request.getToken(), request.getNewPassword()));
        if (!result.isSuccess()) {
            if ("weak_password".equals(result.getErrorCode())) {
                return ResponseEntity.badRequest().body(Map.of("success", false,
                        "message", "La contrasena debe tener al menos 8 caracteres"));
            }
            return ResponseEntity.status(400).body(Map.of("success", false,
                    "message", "El enlace de recuperacion es invalido o expiro"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Contrasena restablecida correctamente"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody ChangePasswordRequest request) {

        String token = bearer(authorizationHeader);
        if (token == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Token no proporcionado"));
        }
        if (tokenBlacklistService.isBlacklisted(token) || !jwtUtil.isTokenValid(token)) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Token invalido o expirado"));
        }
        if (request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Contrasena actual es requerida"));
        }
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Nueva contrasena es requerida"));
        }

        AuthService.PasswordChangeResult result = authService.changePassword(
                jwtUtil.getUserId(token), request.getCurrentPassword(), request.getNewPassword());
        if (!result.isSuccess()) {
            switch (result.getErrorCode()) {
                case "current_password_incorrect":
                    return ResponseEntity.status(401).body(Map.of("success", false, "message", "Contrasena actual incorrecta"));
                case "weak_password":
                    return ResponseEntity.badRequest().body(Map.of("success", false,
                            "message", "La contrasena debe tener al menos 8 caracteres"));
                case "user_not_found":
                    return ResponseEntity.status(404).body(Map.of("success", false, "message", "Usuario no encontrado"));
                default:
                    return ResponseEntity.status(500).body(Map.of("success", false, "message", "Error al cambiar contrasena"));
            }
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Contrasena cambiada correctamente"));
    }

    private static String bearer(String header) {
        return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
    }

    // Request classes
    public static class LoginRequest {
        private String email;
        private String password;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class ForgotPasswordRequest {
        private String email;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class ResetPasswordRequest {
        private String token;
        private String newPassword;
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }
}