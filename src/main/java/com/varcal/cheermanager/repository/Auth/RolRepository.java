package com.varcal.cheermanager.repository.Auth;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.varcal.cheermanager.DTO.RolConConteoDTO;
import com.varcal.cheermanager.Models.Auth.Rol;

@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {

    //Optional<Rol> findById(Rol rol2);
    Optional<Rol> findById(Integer id);


    @Query(value = "SELECT * FROM vista_roles_usuarios", nativeQuery = true)
    List<Object[]> listarRolesConConteo();

    // The findById method is inherited from JpaRepository and does not need to be
    // redefined here.
}
