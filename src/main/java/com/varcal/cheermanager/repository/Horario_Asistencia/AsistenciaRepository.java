package com.varcal.cheermanager.repository.Horario_Asistencia;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.varcal.cheermanager.Models.Horario_Asistencia.Asistencia;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Integer> {

    // Evita registrar dos veces la asistencia del mismo deportista, en el
    // mismo horario, el mismo día.
    Optional<Asistencia> findByDeportistaIdAndHorarioIdAndFecha(Integer deportistaId, Integer horarioId, LocalDate fecha);

    List<Asistencia> findByHorarioIdAndFecha(Integer horarioId, LocalDate fecha);

    List<Asistencia> findByDeportistaIdOrderByFechaDesc(Integer deportistaId);

    List<Asistencia> findByDeportistaIdAndFechaBetweenOrderByFechaDesc(Integer deportistaId, LocalDate desde, LocalDate hasta);

    // Asistencias de un grupo completo en una fecha: cruza con
    // DeportistaPerteneceGrupo a través del deportista, y con
    // HorarioEntrenamiento a través del horario, para asegurarse de traer
    // solo lo que realmente corresponde a ese grupo (y no a otro grupo que
    // por coincidencia comparta el mismo bloque de horario).
    @Query("SELECT a FROM Asistencia a "
            + "JOIN HorarioEntrenamiento he ON he.horario = a.horario "
            + "WHERE he.grupoEntrenamiento.id = :grupoId AND a.fecha = :fecha")
    List<Asistencia> findByGrupoIdAndFecha(@Param("grupoId") Integer grupoId, @Param("fecha") LocalDate fecha);

    // Porcentaje de asistencia de un deportista en un rango: cuenta cuántos
    // registros tiene con un estado cuyo nombre coincide con el dado (ej.
    // "Presente"), sobre el total de registros en ese rango.
    @Query("SELECT COUNT(a) FROM Asistencia a WHERE a.deportista.id = :deportistaId "
            + "AND a.fecha BETWEEN :desde AND :hasta AND a.estadoAsistencia.estado = :estado")
    long contarPorDeportistaYEstadoEnRango(@Param("deportistaId") Integer deportistaId,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta, @Param("estado") String estado);

    long countByDeportistaIdAndFechaBetween(Integer deportistaId, LocalDate desde, LocalDate hasta);
}