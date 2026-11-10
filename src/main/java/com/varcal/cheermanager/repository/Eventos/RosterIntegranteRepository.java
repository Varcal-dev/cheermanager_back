package com.varcal.cheermanager.repository.Eventos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Eventos.RosterIntegrante;

public interface RosterIntegranteRepository extends JpaRepository<RosterIntegrante, Integer> {
    List<RosterIntegrante> findByRosterIdOrderByRolAscIdAsc(Integer rosterId);
}
