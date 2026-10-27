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

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    long countByUsernameStartingWith(String base);

    /**
     * Reactiva el usuario desactivado de una persona. Es SQL nativo a proposito: Usuario lleva
     * @SQLRestriction("activo = true"), asi que JPA no "ve" los usuarios desactivados. Al ser nativo
     * tampoco aplica el filtro de tenant de Hibernate, por eso el club se pasa y se filtra a mano.
     * Devuelve cuantas filas reactivo (0 si la persona no tiene usuario o ya estaba activo).
     */
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(
            value = "UPDATE usuarios SET activo = true WHERE persona_id = :personaId AND club_id = :clubId AND activo = false",
            nativeQuery = true)
    int reactivarPorPersona(@Param("personaId") Integer personaId, @Param("clubId") Integer clubId);

}