package com.varcal.cheermanager.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.repository.Auth.RolRepository;
import com.varcal.cheermanager.repository.Auth.UserRepository;

@Configuration
public class DataInitializer {
    
    @Bean
    CommandLineRunner initAdmin(UserRepository usuarioRepo,
                                RolRepository rolRepo,
                                PasswordEncoder passwordEncoder) {
        return args -> {

            // Validar si ya existe un admin
            if (usuarioRepo.existsByUsername("admin")) {
                return;
            }

            // Buscar rol ADMIN
            Rol adminRol = rolRepo.findById(1)
                    .orElseThrow(() -> new RuntimeException("Rol ADMIN no existe"));

            // Crear usuario admin
            Usuario admin = new Usuario();
            admin.setUsername("admin");
            admin.setEmail("admin@cheer.com");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setRol(adminRol);

            usuarioRepo.save(admin);

            System.out.println("✅ Admin inicial creado");
        };
    }
}