package com.varcal.cheermanager.repository.Eventos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Eventos.PremioEvento;

public interface PremioEventoRepository extends JpaRepository<PremioEvento, Integer> {
    List<PremioEvento> findByEventoId(Integer eventoId);
}