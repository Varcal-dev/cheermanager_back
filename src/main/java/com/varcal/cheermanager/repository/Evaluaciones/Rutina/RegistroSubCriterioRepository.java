package com.varcal.cheermanager.repository.Evaluaciones.Rutina;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Evaluaciones.Rutina.RegistroSubCriterio;

public interface RegistroSubCriterioRepository extends JpaRepository<RegistroSubCriterio, Integer> {
    List<RegistroSubCriterio> findByEvaluacionRutinaId(Integer evaluacionRutinaId);
}