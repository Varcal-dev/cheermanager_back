package com.varcal.cheermanager.repository.Auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.varcal.cheermanager.Models.Auth.Rol;

@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {
}
