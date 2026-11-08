package com.varcal.cheermanager.repository.Personas;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Personas.HoraEntrenador;

public interface HoraEntrenadorRepository extends JpaRepository<HoraEntrenador, Integer> {
    List<HoraEntrenador> findByFechaBetweenOrderByFechaDescIdDesc(LocalDate desde, LocalDate hasta);
}
