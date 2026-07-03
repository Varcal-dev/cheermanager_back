package com.varcal.cheermanager.Controller.Inventario;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.DTO.Inventario.ProductoVarianteDTO;
import com.varcal.cheermanager.DTO.Inventario.ProductoVarianteResponseDTO;
import com.varcal.cheermanager.Service.Inventario.ProductoVarianteService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/producto-variantes")
public class ProductoVarianteController {

    @Autowired
    private ProductoVarianteService productoVarianteService;

    @GetMapping("/producto/{productoId}")
    @RequiresPermission("ver_producto")
    public ResponseEntity<List<ProductoVarianteResponseDTO>> listarPorProducto(@PathVariable Integer productoId) {
        return ResponseEntity.ok(productoVarianteService.listarPorProducto(productoId));
    }

    // Umbral configurable por query param para que cada dashboard/reporte
    // decida qué considera "stock bajo" sin tocar código (ej. ?umbral=5).
    @GetMapping("/stock-bajo")
    @RequiresPermission("ver_producto")
    public ResponseEntity<List<ProductoVarianteResponseDTO>> listarConStockBajo(
            @RequestParam(defaultValue = "5") int umbral) {
        return ResponseEntity.ok(productoVarianteService.listarConStockBajo(umbral));
    }

    @PostMapping
    @RequiresPermission("crear_producto")
    public ResponseEntity<?> crear(@RequestBody ProductoVarianteDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(productoVarianteService.crear(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/precio")
    @RequiresPermission("modificar_producto")
    public ResponseEntity<?> actualizarPrecio(@PathVariable Integer id, @RequestBody java.util.Map<String, java.math.BigDecimal> body) {
        try {
            return ResponseEntity.ok(productoVarianteService.actualizarPrecio(id, body.get("precioVenta")));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("eliminar_producto")
    public ResponseEntity<?> desactivar(@PathVariable Integer id) {
        try {
            productoVarianteService.desactivar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}