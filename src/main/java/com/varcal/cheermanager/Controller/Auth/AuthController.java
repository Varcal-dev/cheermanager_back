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
}