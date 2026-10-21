package com.varcal.cheermanager.repository.Auth;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.varcal.cheermanager.Models.Auth.Rol;

@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {

    // Optional<Rol> findById(Rol rol2);
    @NonNull
    Optional<Rol> findById(Integer id);

    // JPQL (no nativa): Hibernate agrega el filtro de club a Rol y a Usuario automaticamente.
    @Query("SELECT r.id, r.nombre, COUNT(u.id) FROM Rol r LEFT JOIN Usuario u ON u.rol = r GROUP BY r.id, r.nombre")
    List<Object[]> listarRolesConConteo();

    Optional<Rol> findByNombre(String nombre);

    // The findById method is inherited from JpaRepository and does not need to be
    // redefined here.
}