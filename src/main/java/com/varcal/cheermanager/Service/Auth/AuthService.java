package com.varcal.cheermanager.Service.Auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.security.JwtUtil;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    public LoginResult authenticate(String email, String password) {
        return userRepository.findByEmail(email).map(user -> {
            try {
                boolean isAuthenticated = BCrypt.checkpw(password, user.getPasswordHash());
                if (isAuthenticated) {
                    userRepository.actualizar_ultimo_acceso(user.getId(), java.time.LocalDateTime.now());

                    Rol rol = user.getRol();
                    List<String> permisos = (rol != null && rol.getPermisos() != null)
                            ? rol.getPermisos().stream().map(p -> p.getNombre()).collect(Collectors.toList())
                            : List.of();

                    String token = jwtUtil.generateToken(user.getId().longValue(), user.getEmail(),
                            rol != null ? rol.getNombre() : "", permisos);

                    return new LoginResult(true, token, null);
                } else {
                    return new LoginResult(false, null, "invalid_password");
                }
            } catch (IllegalArgumentException e) {
                return new LoginResult(false, null, "invalid_hash");
            }
        }).orElse(new LoginResult(false, null, "email_not_found"));
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