package com.varcal.cheermanager.repository.Eventos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Eventos.ViajeCompetencia;

public interface ViajeCompetenciaRepository extends JpaRepository<ViajeCompetencia, Integer> {
    Optional<ViajeCompetencia> findByEventoId(Integer eventoId);
    List<ViajeCompetencia> findAllByOrderByFechaSalidaDesc();
}
