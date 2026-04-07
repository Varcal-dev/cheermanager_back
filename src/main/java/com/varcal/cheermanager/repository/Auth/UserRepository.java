package com.varcal.cheermanager.repository.Auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Personas.Persona;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByPersona(Persona persona);
    Optional<Usuario> findByTokenResetPassword(String token);

    @Procedure(name = "actualizar_ultimo_acceso")
    void actualizar_ultimo_acceso(@Param("p_usuario_id") Integer usuarioId,
            @Param("p_fecha_acceso") LocalDateTime fechaAcceso);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    long countByUsernameStartingWith(String base);

}