package com.varcal.cheermanager.repository.Evaluaciones.Rutina;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Evaluaciones.Rutina.TopeSeccionNivel;

public interface TopeSeccionNivelRepository extends JpaRepository<TopeSeccionNivel, Integer> {

    // Ordenado descendente porque las calculadoras recorren de mayor a menor
    // escalón y se quedan con el primero que cumpla las condiciones.
    List<TopeSeccionNivel> findByNivelIdAndSubCriterioIdOrderByOrdenDesc(Integer nivelId, Integer subCriterioId);
}