package com.varcal.cheermanager.repository.Inventario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Inventario.CategoriaProducto;

public interface CategoriaProductoRepository extends JpaRepository<CategoriaProducto, Integer> {
    Optional<CategoriaProducto> findByCodigo(String codigo);
    Optional<CategoriaProducto> findByNombre(String nombre);
}