package com.varcal.cheermanager.repository.Eventos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Eventos.GrupoEvento;

public interface GrupoEventoRepository extends JpaRepository<GrupoEvento, Integer> {

    List<GrupoEvento> findByEventoId(Integer eventoId);

    // Historial de competencias de un grupo, para armar su línea de tiempo.
    List<GrupoEvento> findByGrupoEntrenamientoId(Integer grupoEntrenamientoId);

    boolean existsByEventoIdAndGrupoEntrenamientoId(Integer eventoId, Integer grupoEntrenamientoId);
}