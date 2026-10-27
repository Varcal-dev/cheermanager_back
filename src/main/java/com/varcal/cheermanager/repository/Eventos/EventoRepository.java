package com.varcal.cheermanager.repository.Eventos;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

import com.varcal.cheermanager.Models.Eventos.Evento;

public interface EventoRepository extends JpaRepository<Evento, Integer> {

    // Calendario: próximos eventos desde hoy en adelante, ordenados.
    List<Evento> findByFechaGreaterThanEqualOrderByFechaAsc(LocalDate desde);

    // Historial: eventos ya pasados, más recientes primero.
    List<Evento> findByFechaLessThanOrderByFechaDesc(LocalDate hasta);

    List<Evento> findByTipoEventoIdOrderByFechaDesc(Integer tipoEventoId);

    List<Evento> findByFechaBetweenOrderByFechaAsc(LocalDate desde, LocalDate hasta);

    // Bloqueo pesimista: serializa dos "cobrar evento" simultaneos sobre el MISMO evento,
    // para que no se facture dos veces al mismo deportista.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Evento e WHERE e.id = :id")
    Optional<Evento> findByIdForUpdate(@Param("id") Integer id);
}