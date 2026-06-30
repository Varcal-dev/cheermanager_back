package com.varcal.cheermanager.repository.Evaluaciones.Rutina;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Evaluaciones.Rutina.NivelCompetencia;

public interface NivelCompetenciaRepository extends JpaRepository<NivelCompetencia, Integer> {
    Optional<NivelCompetencia> findByNombre(String nombre);
}