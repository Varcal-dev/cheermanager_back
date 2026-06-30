package com.varcal.cheermanager.repository.Horario_Asistencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Horario_Asistencia.HorarioEntrenamiento;

public interface HorarioEntrenamientoRepository extends JpaRepository<HorarioEntrenamiento, Integer> {

    List<HorarioEntrenamiento> findByGrupoEntrenamientoId(Integer grupoEntrenamientoId);

    List<HorarioEntrenamiento> findByHorarioId(Integer horarioId);

    boolean existsByHorarioIdAndGrupoEntrenamientoId(Integer horarioId, Integer grupoEntrenamientoId);
}