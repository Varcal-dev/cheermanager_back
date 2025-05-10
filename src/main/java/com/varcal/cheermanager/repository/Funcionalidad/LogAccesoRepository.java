package com.varcal.cheermanager.repository.Funcionalidad;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Funcionalidad.LogAcceso;

public interface LogAccesoRepository extends JpaRepository<LogAcceso, Long> {
    // Métodos adicionales si son necesarios
}