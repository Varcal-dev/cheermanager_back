package com.varcal.cheermanager.Service.Auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.repository.Auth.UserRepository;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    public String authenticate(String email, String password, HttpServletRequest request) {
        return userRepository.findByEmail(email).map(user -> {
            try {
                boolean isAuthenticated = BCrypt.checkpw(password, user.getPasswordHash());
                if (isAuthenticated) {
                    // Llamar al procedimiento almacenado para actualizar el último acceso
                    userRepository.actualizar_ultimo_acceso(user.getId(), java.time.LocalDateTime.now());

                    // Guardar información del usuario en la sesión
                    HttpSession session = request.getSession();
                    session.setAttribute("userId", user.getId());
                    session.setAttribute("email", user.getEmail());
                    return "success"; // Login exitoso
                } else {
                    return "invalid_password"; // Contraseña incorrecta
                }
            } catch (IllegalArgumentException e) {
                return "invalid_hash"; // Hash inválido
            }
        }).orElse("email_not_found"); // Correo no encontrado
    }

    public boolean tienePermiso(int userId, String permisoRequerido) {
        return userRepository.findById((long) userId).map(user -> {
            return user.getRol().getPermisos().stream()
                    .anyMatch(permiso -> permiso.getNombre().equals(permisoRequerido));
        }).orElse(false);
    }

    public Set<String> getPermisosUsuario(int userId) {
        return userRepository.findById((long) userId).map(user -> {
            // Obtener los nombres de los permisos asociados al rol del usuario
            return user.getRol().getPermisos().stream()
                    .map(permiso -> permiso.getNombre())
                    .collect(Collectors.toSet());
        }).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

   

}