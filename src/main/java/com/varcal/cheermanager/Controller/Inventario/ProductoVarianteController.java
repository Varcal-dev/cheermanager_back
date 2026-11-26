package com.varcal.cheermanager.Controller.Inventario;

import jakarta.validation.Valid;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.config.ErrorPublico;
import com.varcal.cheermanager.DTO.Inventario.ActualizarPrecioDTO;
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
    public ResponseEntity<?> crear(@Valid @RequestBody ProductoVarianteDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(productoVarianteService.crear(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ErrorPublico.de(e));
        }
    }

    @PutMapping("/{id}/precio")
    @RequiresPermission("modificar_producto")
    public ResponseEntity<?> actualizarPrecio(@PathVariable Integer id, @Valid @RequestBody ActualizarPrecioDTO body) {
        try {
            return ResponseEntity.ok(productoVarianteService.actualizarPrecio(id, body.precioVenta()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ErrorPublico.de(e));
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