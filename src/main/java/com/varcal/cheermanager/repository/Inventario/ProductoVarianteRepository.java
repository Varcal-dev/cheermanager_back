package com.varcal.cheermanager.repository.Inventario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.varcal.cheermanager.Models.Inventario.ProductoVariante;

public interface ProductoVarianteRepository extends JpaRepository<ProductoVariante, Integer> {
    List<ProductoVariante> findByProductoId(Integer productoId);
    Optional<ProductoVariante> findBySkuVariante(String skuVariante);
    boolean existsBySkuVariante(String skuVariante);

    // Stock bajo: solo tiene sentido para variantes de productos que sí
    // controlan stock (las de DIG tienen stockActual null y quedan afuera
    // automáticamente por la comparación numérica).
    @Query("SELECT v FROM ProductoVariante v WHERE v.producto.requiereControlStock = true " +
           "AND v.stockActual IS NOT NULL AND v.stockActual <= :umbral AND v.activo = true")
    List<ProductoVariante> findConStockBajo(int umbral);
}