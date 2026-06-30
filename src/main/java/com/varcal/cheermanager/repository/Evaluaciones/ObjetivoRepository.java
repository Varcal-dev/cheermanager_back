package com.varcal.cheermanager.repository.Evaluaciones;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Evaluaciones.Objetivo;

public interface ObjetivoRepository extends JpaRepository<Objetivo, Integer> {
    List<Objetivo> findByDeportistaIdOrderByFechaCreacionDesc(Integer deportistaId);

    List<Objetivo> findByDeportistaIdAndEstadoObjetivoId(Integer deportistaId, Integer estadoObjetivoId);

    List<Objetivo> findByEstadoObjetivoId(Integer estadoObjetivoId);
}