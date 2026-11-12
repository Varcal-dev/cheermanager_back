package com.varcal.cheermanager.repository.Evaluaciones;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Evaluaciones.EvaluacionPlantilla;

public interface EvaluacionPlantillaRepository extends JpaRepository<EvaluacionPlantilla, Integer> {
    List<EvaluacionPlantilla> findByActivaTrueOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);
}
