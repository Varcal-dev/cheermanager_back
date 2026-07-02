package com.varcal.cheermanager.repository.Eventos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.varcal.cheermanager.Models.Eventos.ResultadoCompetencia;

public interface ResultadoCompetenciaRepository extends JpaRepository<ResultadoCompetencia, Integer> {

    List<ResultadoCompetencia> findByEventoIdOrderByPosicionAsc(Integer eventoId);

    // Línea de tiempo de un grupo evento-a-evento, más antiguo primero — es
    // la fuente para el KPI de "progresión de puntaje" que se propuso.
    @Query("SELECT r FROM ResultadoCompetencia r WHERE r.grupoEntrenamiento.id = :grupoId " +
           "ORDER BY r.evento.fecha ASC")
    List<ResultadoCompetencia> findProgresionPorGrupo(@Param("grupoId") Integer grupoId);

    Optional<ResultadoCompetencia> findByEventoIdAndGrupoEntrenamientoId(Integer eventoId, Integer grupoEntrenamientoId);

    boolean existsByPremioId(Integer premioId);

    List<ResultadoCompetencia> findByGrupoEntrenamientoId(Integer grupoEntrenamientoId);
}