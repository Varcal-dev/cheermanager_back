package com.varcal.cheermanager.repository.Evaluaciones.Habilidades;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Evaluaciones.Habilidades.EvaluacionHabilidad;

public interface EvaluacionHabilidadRepository extends JpaRepository<EvaluacionHabilidad, Integer> {
    List<EvaluacionHabilidad> findByDeportistaIdAndHabilidadIdOrderByFechaDescIdDesc(Integer deportistaId, Integer habilidadId);
}
