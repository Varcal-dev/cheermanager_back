package com.varcal.cheermanager.repository.Evaluaciones;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Evaluaciones.EstadoObjetivo;

public interface EstadoObjetivoRepository extends JpaRepository<EstadoObjetivo, Integer> {
    Optional<EstadoObjetivo> findByNombreEstado(String nombreEstado);
}