package com.varcal.cheermanager.repository.Eventos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Eventos.RosterCompetencia;

public interface RosterCompetenciaRepository extends JpaRepository<RosterCompetencia, Integer> {
    Optional<RosterCompetencia> findByEventoIdAndGrupoId(Integer eventoId, Integer grupoId);
}
