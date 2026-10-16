package com.varcal.cheermanager.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.repository.Auth.RolRepository;
import com.varcal.cheermanager.repository.Auth.UserRepository;

/**
 * Crea el administrador inicial SOLO si:
 *  - no existe ya un usuario con ese username, y
 *  - se definio APP_BOOTSTRAP_ADMIN_PASSWORD (minimo 10 caracteres).
 * Ya no existe una clave por defecto: sin la variable, no se crea nada.
 * El admin queda con "requiereCambioPassword" para forzar su cambio en el primer login.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initAdmin(UserRepository usuarioRepo,
                                RolRepository rolRepo,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.bootstrap.admin-username:admin}") String username,
                                @Value("${app.bootstrap.admin-email:}") String email,
                                @Value("${app.bootstrap.admin-password:}") String password) {
        return args -> {
            if (usuarioRepo.existsByUsername(username)) {
                return;
            }
            if (password == null || password.isBlank()) {
                log.warn("No existe el usuario '{}' y APP_BOOTSTRAP_ADMIN_PASSWORD esta vacio: "
                        + "no se crea ningun administrador inicial.", username);
                return;
            }
            if (password.length() < 10) {
                throw new IllegalStateException(
                        "APP_BOOTSTRAP_ADMIN_PASSWORD debe tener al menos 10 caracteres");
            }
            if (email == null || email.isBlank()) {
                throw new IllegalStateException(
                        "Define APP_BOOTSTRAP_ADMIN_EMAIL para crear el administrador inicial");
            }

            Rol adminRol = rolRepo.findByNombre("ADMIN")
                    .or(() -> rolRepo.findByNombre("Admin"))
                    .or(() -> rolRepo.findById(1))
                    .orElseThrow(() -> new IllegalStateException(
                            "No existe el rol ADMIN: cargalo (sql/fase0_permisos.sql) antes de crear el admin"));

            Usuario admin = new Usuario();
            admin.setUsername(username);
            admin.setEmail(email);
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setRol(adminRol);
            admin.setRequiereCambioPassword(true);
            usuarioRepo.save(admin);

            log.info("Administrador inicial '{}' creado (debe cambiar la clave en el primer login).", username);
        };
    }
}
