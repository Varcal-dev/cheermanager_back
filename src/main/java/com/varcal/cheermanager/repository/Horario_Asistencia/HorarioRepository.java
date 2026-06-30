package com.varcal.cheermanager.repository.Horario_Asistencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.varcal.cheermanager.Models.Horario_Asistencia.Horario;

public interface HorarioRepository extends JpaRepository<Horario, Integer> {

    List<Horario> findByDiaId(Integer diaId);

    // Todos los bloques de horario asignados a un grupo en particular,
    // pasando por la tabla puente HorarioEntrenamiento.
    @Query("SELECT h FROM Horario h JOIN HorarioEntrenamiento he ON he.horario = h "
            + "WHERE he.grupoEntrenamiento.id = :grupoId ORDER BY h.dia.id, h.horaInicio")
    List<Horario> findByGrupoEntrenamientoId(@Param("grupoId") Integer grupoId);
}