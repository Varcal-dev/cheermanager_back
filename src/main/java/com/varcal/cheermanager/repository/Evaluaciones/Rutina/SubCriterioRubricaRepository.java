package com.varcal.cheermanager.repository.Evaluaciones.Rutina;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Evaluaciones.Rutina.SubCriterioRubrica;

public interface SubCriterioRubricaRepository extends JpaRepository<SubCriterioRubrica, Integer> {
    Optional<SubCriterioRubrica> findByNombre(String nombre);
    List<SubCriterioRubrica> findBySeccion(String seccion);
}