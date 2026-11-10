package com.varcal.cheermanager.repository.Eventos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Eventos.ViajeParticipante;

public interface ViajeParticipanteRepository extends JpaRepository<ViajeParticipante, Integer> {
    List<ViajeParticipante> findByViajeIdOrderByIdAsc(Integer viajeId);
}
