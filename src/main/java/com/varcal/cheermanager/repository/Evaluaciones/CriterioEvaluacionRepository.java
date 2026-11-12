package com.varcal.cheermanager.repository.Evaluaciones;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Evaluaciones.CriterioEvaluacion;

public interface CriterioEvaluacionRepository extends JpaRepository<CriterioEvaluacion, Integer> {
    List<CriterioEvaluacion> findByCategoriaId(Integer categoriaId);

    // Nombre repetido dentro de la misma categoria (sin distinguir mayusculas) en este club.
    boolean existsByCategoriaIdAndNombreIgnoreCase(Integer categoriaId, String nombre);

    Optional<CriterioEvaluacion> findFirstByCategoriaIdAndNombreIgnoreCase(Integer categoriaId, String nombre);

    boolean existsByCategoriaIdAndNombreIgnoreCaseAndIdNot(Integer categoriaId, String nombre, Integer id);
}