package com.varcal.cheermanager.repository.Evaluaciones;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.varcal.cheermanager.Models.Evaluaciones.Evaluacion;

public interface EvaluacionRepository extends JpaRepository<Evaluacion, Integer> {

    List<Evaluacion> findByDeportistaIdOrderByFechaDesc(Integer deportistaId);

    List<Evaluacion> findByDeportistaIdAndCriterioEvaluacionIdOrderByFechaAsc(Integer deportistaId, Integer criterioId);

    List<Evaluacion> findByDeportistaIdAndFechaBetweenOrderByFechaDesc(Integer deportistaId, LocalDate desde, LocalDate hasta);

    // Última evaluación de un deportista para cada criterio de una categoría
    // (ej. "Gimnasia"), para construir una foto del estado técnico actual sin
    // traer todo el historial completo.
    @Query("SELECT e FROM Evaluacion e WHERE e.deportista.id = :deportistaId "
            + "AND e.criterioEvaluacion.categoria.id = :categoriaId "
            + "AND e.fecha = (SELECT MAX(e2.fecha) FROM Evaluacion e2 "
            + "WHERE e2.deportista.id = e.deportista.id AND e2.criterioEvaluacion.id = e.criterioEvaluacion.id)")
    List<Evaluacion> findUltimaEvaluacionPorCategoria(@Param("deportistaId") Integer deportistaId,
            @Param("categoriaId") Integer categoriaId);

    @Query("SELECT AVG(e.puntajeObtenido) FROM Evaluacion e WHERE e.deportista.id = :deportistaId "
            + "AND e.criterioEvaluacion.id = :criterioId")
    Double promedioPorCriterio(@Param("deportistaId") Integer deportistaId, @Param("criterioId") Integer criterioId);
}