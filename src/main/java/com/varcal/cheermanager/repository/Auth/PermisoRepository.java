package com.varcal.cheermanager.repository.Auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.varcal.cheermanager.Models.Auth.Permiso;

@Repository
public interface PermisoRepository extends JpaRepository<Permiso, Integer> {
}