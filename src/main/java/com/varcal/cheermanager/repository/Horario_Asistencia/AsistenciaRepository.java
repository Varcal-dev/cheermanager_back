package com.varcal.cheermanager.repository.Horario_Asistencia;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.varcal.cheermanager.Models.Horario_Asistencia.Asistencia;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Integer> {

    Optional<Asistencia> findByDeportistaIdAndHorarioIdAndFecha(Integer deportistaId, Integer horarioId, LocalDate fecha);

    List<Asistencia> findByHorarioIdAndFecha(Integer horarioId, LocalDate fecha);

    List<Asistencia> findByDeportistaIdOrderByFechaDesc(Integer deportistaId);

    List<Asistencia> findByDeportistaIdAndFechaBetweenOrderByFechaDesc(Integer deportistaId, LocalDate desde, LocalDate hasta);

    @Query("SELECT a FROM Asistencia a "
            + "JOIN HorarioEntrenamiento he ON he.horario = a.horario "
            + "WHERE he.grupoEntrenamiento.id = :grupoId AND a.fecha = :fecha")
    List<Asistencia> findByGrupoIdAndFecha(@Param("grupoId") Integer grupoId, @Param("fecha") LocalDate fecha);

    @Query("SELECT COUNT(a) FROM Asistencia a WHERE a.deportista.id = :deportistaId "
            + "AND a.fecha BETWEEN :desde AND :hasta AND a.estadoAsistencia.estado = :estado")
    long contarPorDeportistaYEstadoEnRango(@Param("deportistaId") Integer deportistaId,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta, @Param("estado") String estado);

    long countByDeportistaIdAndFechaBetween(Integer deportistaId, LocalDate desde, LocalDate hasta);

    // --- Agregados nuevos para el dashboard de Horarios ---

    // Total de registros de asistencia de un grupo en un rango (base del %).
    @Query("SELECT COUNT(a) FROM Asistencia a JOIN HorarioEntrenamiento he ON he.horario = a.horario "
            + "WHERE he.grupoEntrenamiento.id = :grupoId AND a.fecha BETWEEN :desde AND :hasta")
    long contarTotalPorGrupoEnRango(@Param("grupoId") Integer grupoId,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    // Registros con un estado específico (ej. "Presente") de un grupo en un rango.
    @Query("SELECT COUNT(a) FROM Asistencia a JOIN HorarioEntrenamiento he ON he.horario = a.horario "
            + "WHERE he.grupoEntrenamiento.id = :grupoId AND a.fecha BETWEEN :desde AND :hasta "
            + "AND a.estadoAsistencia.estado = :estado")
    long contarPorGrupoYEstadoEnRango(@Param("grupoId") Integer grupoId,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta, @Param("estado") String estado);

    // Igual que las dos anteriores pero sin filtrar por grupo — asistencia
    // general del club completo, para el resumen del dashboard.
    @Query("SELECT COUNT(a) FROM Asistencia a WHERE a.fecha BETWEEN :desde AND :hasta")
    long contarTotalEnRango(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("SELECT COUNT(a) FROM Asistencia a WHERE a.fecha BETWEEN :desde AND :hasta AND a.estadoAsistencia.estado = :estado")
    long contarPorEstadoEnRango(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta, @Param("estado") String estado);
}