package com.varcal.cheermanager.Controller.Auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.Service.Auth.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String authResult = authService.authenticate(loginRequest.getEmail(), loginRequest.getPassword(), request);

        switch (authResult) {
            case "success":
                return ResponseEntity.ok(new LoginResponse(true, "Login exitoso"));

            case "invalid_password":
                return ResponseEntity.status(401).body(new LoginResponse(false, "Contraseña incorrecta"));

            case "email_not_found":
                return ResponseEntity.status(404).body(new LoginResponse(false, "El correo proporcionado no está registrado"));

            case "invalid_hash":
                return ResponseEntity.status(500).body(new LoginResponse(false, "Error en el formato del hash de la contraseña"));

            default:
                return ResponseEntity.status(500).body(new LoginResponse(false, "Error desconocido"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Sesión cerrada exitosamente");
    }

    // Clase interna para manejar el cuerpo de la solicitud
    public static class LoginRequest {
        private String email;
        private String password;

        // Getters y setters
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

    // Clase interna para manejar la respuesta
    public static class LoginResponse {
        private boolean success;
        private String message;

        public LoginResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}