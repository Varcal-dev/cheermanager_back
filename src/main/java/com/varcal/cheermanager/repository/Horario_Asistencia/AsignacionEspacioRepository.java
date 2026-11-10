package com.varcal.cheermanager.repository.Horario_Asistencia;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Horario_Asistencia.AsignacionEspacio;

public interface AsignacionEspacioRepository extends JpaRepository<AsignacionEspacio, Integer> {
    Optional<AsignacionEspacio> findByHorarioEntrenamientoId(Integer horarioEntrenamientoId);
    List<AsignacionEspacio> findByEspacioId(Integer espacioId);
}
