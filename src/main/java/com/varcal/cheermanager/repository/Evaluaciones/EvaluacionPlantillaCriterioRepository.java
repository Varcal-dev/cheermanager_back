package com.varcal.cheermanager.repository.Evaluaciones;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Evaluaciones.EvaluacionPlantillaCriterio;

public interface EvaluacionPlantillaCriterioRepository extends JpaRepository<EvaluacionPlantillaCriterio, Integer> {
    List<EvaluacionPlantillaCriterio> findByPlantillaIdOrderByOrdenAsc(Integer plantillaId);

    void deleteByPlantillaId(Integer plantillaId);
}
