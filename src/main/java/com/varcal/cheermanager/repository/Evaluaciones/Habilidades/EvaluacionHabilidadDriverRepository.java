package com.varcal.cheermanager.repository.Evaluaciones.Habilidades;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Evaluaciones.Habilidades.EvaluacionHabilidadDriver;

public interface EvaluacionHabilidadDriverRepository extends JpaRepository<EvaluacionHabilidadDriver, Integer> {
    List<EvaluacionHabilidadDriver> findByEvaluacionHabilidadIdIn(Collection<Integer> evaluacionIds);
}
