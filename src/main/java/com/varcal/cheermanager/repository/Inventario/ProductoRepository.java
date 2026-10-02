package com.varcal.cheermanager.repository.Inventario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.varcal.cheermanager.Models.Inventario.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    Optional<Producto> findBySku(String sku);
    List<Producto> findByCategoriaProductoId(Integer categoriaId);
    List<Producto> findByActivoTrue();
    List<Producto> findByRequiereControlStockTrue();
    boolean existsBySku(String sku);

    // Búsqueda server-side: el filtrado lo hace MySQL, no el frontend
    // cargando todo el catálogo y recorriéndolo en el navegador. Busca en
    // nombre, SKU y nombre de categoría a la vez, sin importar mayúsculas.
    @Query("SELECT p FROM Producto p WHERE " +
           "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "LOWER(p.categoriaProducto.nombre) LIKE LOWER(CONCAT('%', :termino, '%'))")
    List<Producto> buscar(@Param("termino") String termino);
}