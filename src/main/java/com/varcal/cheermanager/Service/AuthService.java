package com.varcal.cheermanager.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.repository.Auth.UserRepository;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    public boolean authenticate(String email, String password, HttpServletRequest request) {
        return userRepository.findByEmail(email).map(user -> {
            boolean isAuthenticated = BCrypt.checkpw(password, user.getPasswordHash());
            if (isAuthenticated) {
                // Llamar al procedimiento almacenado para actualizar el último acceso
                userRepository.actualizar_ultimo_acceso(user.getId(), java.time.LocalDateTime.now());

                // Guardar información del usuario en la sesión
                HttpSession session = request.getSession();
                session.setAttribute("userId", user.getId());
                session.setAttribute("email", user.getEmail());
            }
            return isAuthenticated;
        }).orElse(false);
    }
}