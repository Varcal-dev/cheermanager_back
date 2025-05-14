package com.varcal.cheermanager.repository.Historiales;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Historiales.HistorialMedico;

public interface HistorialMedicoRepository extends JpaRepository<HistorialMedico, Integer> {
    // Métodos personalizados, si es necesario
}
