package com.varcal.cheermanager.repository.Inventario;

import java.time.LocalDate;
import java.util.List;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.varcal.cheermanager.Models.Inventario.Venta;

import jakarta.persistence.LockModeType;

public interface VentaRepository extends JpaRepository<Venta, Integer> {
    List<Venta> findByFechaBetweenOrderByFechaDesc(LocalDate desde, LocalDate hasta);

    /** Para reportes e ingresos: las ventas anuladas no cuentan. */
    List<Venta> findByFechaBetweenAndAnuladaFalseOrderByFechaDesc(LocalDate desde, LocalDate hasta);

    /** Bloquea la venta: evita que dos anulaciones simultaneas devuelvan el stock dos veces. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Venta v where v.id = :id")
    Optional<Venta> findByIdForUpdate(@Param("id") Integer id);
    List<Venta> findByPersonaIdOrderByFechaDesc(Integer personaId);
}