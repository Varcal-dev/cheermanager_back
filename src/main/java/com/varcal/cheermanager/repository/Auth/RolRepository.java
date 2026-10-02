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

    @Query(value = "SELECT \n" + //
            "        `r`.`id` AS `id`,\n" + //
            "        `r`.`nombre` AS `nombre`,\n" + //
            "        COUNT(`u`.`id`) AS `usuarios`\n" + //
            "    FROM\n" + //
            "        `cheermanager_db`.`roles` `r`\n" + //
            "        LEFT JOIN `cheermanager_db`.`usuarios` `u` ON `r`.`id` = `u`.`rol_id`\n" + //
            "    GROUP BY `r`.`id`, `r`.`nombre`", nativeQuery = true)
    List<Object[]> listarRolesConConteo();

    Optional<Rol> findByNombre(String nombre);

    // The findById method is inherited from JpaRepository and does not need to be
    // redefined here.
}
