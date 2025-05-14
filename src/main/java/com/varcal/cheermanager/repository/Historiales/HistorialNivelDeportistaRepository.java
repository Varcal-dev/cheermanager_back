package com.varcal.cheermanager.repository.Historiales;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Historiales.HistorialNivelDeportista;

public interface HistorialNivelDeportistaRepository extends JpaRepository<HistorialNivelDeportista, Integer> {
    // Métodos personalizados, si es necesario
}

