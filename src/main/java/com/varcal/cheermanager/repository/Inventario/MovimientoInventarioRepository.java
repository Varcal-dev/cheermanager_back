package com.varcal.cheermanager.repository.Inventario;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Inventario.MovimientoInventario;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Integer> {
    List<MovimientoInventario> findByProductoVarianteIdOrderByFechaDesc(Integer productoVarianteId);
}