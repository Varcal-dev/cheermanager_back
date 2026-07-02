package com.varcal.cheermanager.repository.Eventos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Eventos.TipoEvento;

public interface TipoEventoRepository extends JpaRepository<TipoEvento, Integer> {
    Optional<TipoEvento> findByEvento(String evento);
}