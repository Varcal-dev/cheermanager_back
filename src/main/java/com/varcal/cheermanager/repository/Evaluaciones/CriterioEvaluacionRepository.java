package com.varcal.cheermanager.repository.Evaluaciones;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Evaluaciones.CriterioEvaluacion;

public interface CriterioEvaluacionRepository extends JpaRepository<CriterioEvaluacion, Integer> {
    List<CriterioEvaluacion> findByCategoriaId(Integer categoriaId);
}