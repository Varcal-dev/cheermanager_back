package com.varcal.cheermanager.config;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.varcal.cheermanager.Models.Auth.Permiso;
import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Clubes.Club;
import com.varcal.cheermanager.Service.Clubes.RolesPlantilla;
import com.varcal.cheermanager.repository.Auth.PermisoRepository;
import com.varcal.cheermanager.repository.Auth.RolRepository;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Clubes.ClubRepository;
import com.varcal.cheermanager.tenant.TenantContext;
import com.varcal.cheermanager.tenant.TenantLookupRepository;

/**
 * Arranque de la plataforma:
 *  1. Si no hay ningun club, crea el club "Plataforma" (id 1).
 *  2. Asegura los permisos de plataforma y el rol SUPERADMIN en ese club.
 *  3. Si hay APP_BOOTSTRAP_ADMIN_PASSWORD y el usuario no existe en ningun club, crea el superadmin.
 * Sin clave por defecto: sin la variable no se crea ningun usuario.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initPlataforma(ClubRepository clubRepo, RolRepository rolRepo, PermisoRepository permisoRepo,
            UserRepository userRepo, TenantLookupRepository lookup, PasswordEncoder encoder,
            @Value("${app.bootstrap.admin-username:superadmin}") String username,
            @Value("${app.bootstrap.admin-email:}") String email,
            @Value("${app.bootstrap.admin-password:}") String password) {
        return args -> {
            if (clubRepo.count() == 0) {
                Club plataforma = new Club();
                plataforma.setNombre("Plataforma");
                plataforma.setSlug("plataforma");
                plataforma = clubRepo.save(plataforma);
                if (plataforma.getId() != Club.PLATAFORMA_ID) {
                    throw new IllegalStateException("El club Plataforma debe tener id 1 (quedo " + plataforma.getId()
                            + "). Usa una base de datos vacia o corre sql/fase1_multiclub.sql.");
                }
            }
            if (!clubRepo.existsById(Club.PLATAFORMA_ID)) {
                log.warn("No existe el club Plataforma (id 1): corre sql/fase1_multiclub.sql. No se crea superadmin.");
                return;
            }

            // Permisos globales de plataforma (se crean si faltan; el resto viene del script de datos / migracion V2)
            for (String nombre : RolesPlantilla.SOLO_PLATAFORMA) {
                if (permisoRepo.findAll().stream().noneMatch(x -> nombre.equals(x.getNombre()))) {
                    Permiso nuevo = new Permiso();
                    nuevo.setNombre(nombre);
                    permisoRepo.save(nuevo);
                }
            }

            TenantContext.runAs(Club.PLATAFORMA_ID, () -> {
                // El Super Administrador tiene TODOS los permisos (igual que el script de datos). Si el rol ya existe
                // NO se le quita nada: solo se le agregan los permisos que falten (p. ej. los de una version nueva).
                Rol rol = rolRepo.findByNombre(RolesPlantilla.SUPERADMIN).orElseGet(() -> {
                    Rol r = new Rol();
                    r.setNombre(RolesPlantilla.SUPERADMIN);
                    r.setDescripcion("Administrador global del sistema, configuracion, seguridad y permisos");
                    r.setPermisos(new HashSet<>());
                    return r;
                });
                Set<Permiso> permisos = rol.getPermisos() == null ? new HashSet<>() : new HashSet<>(rol.getPermisos());
                permisos.addAll(permisoRepo.findAll());
                rol.setPermisos(permisos);
                rol = rolRepo.save(rol);

                if (password == null || password.isBlank()) {
                    return;
                }
                if (lookup.usernameExisteEnAlgunClub(username)) {
                    return; // ya existe (puede estar en cualquier club): no se toca
                }
                if (password.length() < 10) {
                    throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_PASSWORD debe tener al menos 10 caracteres");
                }
                if (email == null || email.isBlank() || lookup.emailExisteEnAlgunClub(email)) {
                    log.warn("Define un APP_BOOTSTRAP_ADMIN_EMAIL unico para crear el superadmin.");
                    return;
                }
                Usuario u = new Usuario();
                u.setUsername(username);
                u.setEmail(email);
                u.setPasswordHash(encoder.encode(password));
                u.setRol(rol);
                u.setRequiereCambioPassword(true);
                userRepo.save(u);
                log.info("Superadmin '{}' creado en el club Plataforma.", username);
            });
        };
    }
}
