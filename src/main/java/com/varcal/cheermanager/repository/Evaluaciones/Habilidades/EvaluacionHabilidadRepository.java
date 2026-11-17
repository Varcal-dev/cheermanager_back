package com.varcal.cheermanager.repository.Evaluaciones.Habilidades;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Evaluaciones.Habilidades.EvaluacionHabilidad;

public interface EvaluacionHabilidadRepository extends JpaRepository<EvaluacionHabilidad, Integer> {
    List<EvaluacionHabilidad> findByDeportistaIdAndHabilidadIdOrderByFechaDescIdDesc(Integer deportistaId, Integer habilidadId);

    /** Todas las evaluaciones del club, de la mas antigua a la mas reciente (ranking de habilidades aprendidas, C12). */
    List<EvaluacionHabilidad> findAllByOrderByFechaAscIdAsc();

    /** Evaluaciones de UN deportista, de la mas antigua a la mas reciente (perfil del deportista: habilidades aprendidas por mes). */
    List<EvaluacionHabilidad> findByDeportistaIdOrderByFechaAscIdAsc(Integer deportistaId);
}
