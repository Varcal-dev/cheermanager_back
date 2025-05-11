package com.varcal.cheermanager.repository.Auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.varcal.cheermanager.Models.Auth.Rol;

@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {

    Optional<Rol> findById(Rol rol2);

    // The findById method is inherited from JpaRepository and does not need to be redefined here.
}
