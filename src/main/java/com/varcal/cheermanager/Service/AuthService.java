package com.varcal.cheermanager.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.repository.UserRepository;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    public boolean authenticate(String email, String password) {
        return userRepository.findByEmail(email)
                .map((Usuario user) -> BCrypt.checkpw(password, user.getPasswordHash())) // Comparar contraseñas
                .orElse(false); // Si no se encuentra el usuario, devolver false
    }

    public void registerUser(String email, String password) {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        Usuario user = new Usuario();
        user.setEmail(email);
        user.setPasswordHash(hashedPassword);
        userRepository.save(user);
    }
}
