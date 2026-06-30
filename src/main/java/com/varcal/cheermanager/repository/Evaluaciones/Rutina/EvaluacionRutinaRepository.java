package com.varcal.cheermanager.repository.Evaluaciones.Rutina;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Evaluaciones.Rutina.EvaluacionRutina;

public interface EvaluacionRutinaRepository extends JpaRepository<EvaluacionRutina, Integer> {
    List<EvaluacionRutina> findByGrupoIdOrderByFechaDesc(Integer grupoId);
}