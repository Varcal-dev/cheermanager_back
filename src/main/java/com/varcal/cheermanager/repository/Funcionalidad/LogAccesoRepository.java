package com.varcal.cheermanager.repository.Funcionalidad;

import org.springframework.core.log.LogAccessor;
import org.springframework.data.jpa.repository.JpaRepository;


public interface LogAccesoRepository extends JpaRepository<LogAccessor, Long> {
    // Métodos adicionales si son necesarios
}