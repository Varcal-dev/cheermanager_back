package com.varcal.cheermanager.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.repository.UserRepository;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    public boolean authenticate(String email, String password) {
        return userRepository.findByEmail(email)
                .map(user -> user.getPasswordHash().equals(password)) // Comparar contraseñas
                .orElse(false); // Si no se encuentra el usuario, devolver false
    }
}
