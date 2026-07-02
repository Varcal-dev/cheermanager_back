package com.varcal.cheermanager.repository.Eventos;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Eventos.Evento;

public interface EventoRepository extends JpaRepository<Evento, Integer> {

    // Calendario: próximos eventos desde hoy en adelante, ordenados.
    List<Evento> findByFechaGreaterThanEqualOrderByFechaAsc(LocalDate desde);

    // Historial: eventos ya pasados, más recientes primero.
    List<Evento> findByFechaLessThanOrderByFechaDesc(LocalDate hasta);

    List<Evento> findByTipoEventoIdOrderByFechaDesc(Integer tipoEventoId);

    List<Evento> findByFechaBetweenOrderByFechaAsc(LocalDate desde, LocalDate hasta);
}