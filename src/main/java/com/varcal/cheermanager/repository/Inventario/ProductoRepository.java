package com.varcal.cheermanager.repository.Inventario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Inventario.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    Optional<Producto> findBySku(String sku);
    List<Producto> findByCategoriaProductoId(Integer categoriaId);
    List<Producto> findByActivoTrue();
    List<Producto> findByRequiereControlStockTrue();
    boolean existsBySku(String sku);
}