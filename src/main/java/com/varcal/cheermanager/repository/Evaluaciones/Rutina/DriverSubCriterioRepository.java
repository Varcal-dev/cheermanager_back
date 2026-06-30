package com.varcal.cheermanager.repository.Evaluaciones.Rutina;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Evaluaciones.Rutina.DriverSubCriterio;

public interface DriverSubCriterioRepository extends JpaRepository<DriverSubCriterio, Integer> {
    List<DriverSubCriterio> findBySubCriterioId(Integer subCriterioId);
}