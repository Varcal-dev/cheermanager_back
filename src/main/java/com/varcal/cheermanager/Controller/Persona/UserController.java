package com.varcal.cheermanager.Controller.Persona;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        String email = (String) session.getAttribute("email");

        if (userId == null) {
            return ResponseEntity.status(401).body("No hay usuario autenticado");
        }

        return ResponseEntity.ok("Usuario autenticado: " + email);
    }
}
