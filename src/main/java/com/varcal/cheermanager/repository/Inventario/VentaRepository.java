package com.varcal.cheermanager.repository.Inventario;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Inventario.Venta;

public interface VentaRepository extends JpaRepository<Venta, Integer> {
    List<Venta> findByFechaBetweenOrderByFechaDesc(LocalDate desde, LocalDate hasta);
    List<Venta> findByPersonaIdOrderByFechaDesc(Integer personaId);
}