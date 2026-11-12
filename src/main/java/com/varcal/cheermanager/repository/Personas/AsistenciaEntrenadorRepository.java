package com.varcal.cheermanager.repository.Personas;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.varcal.cheermanager.Models.Personas.AsistenciaEntrenador;

@Repository
public interface AsistenciaEntrenadorRepository extends JpaRepository<AsistenciaEntrenador, Integer> {
    Optional<AsistenciaEntrenador> findByEntrenadorIdAndFecha(Integer entrenadorId, LocalDate fecha);
}
