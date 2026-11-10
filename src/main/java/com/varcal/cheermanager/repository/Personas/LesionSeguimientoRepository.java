package com.varcal.cheermanager.repository.Personas;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Personas.LesionSeguimiento;

public interface LesionSeguimientoRepository extends JpaRepository<LesionSeguimiento, Integer> {
    List<LesionSeguimiento> findByLesionIdOrderByIdAsc(Integer lesionId);
}
