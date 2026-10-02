package com.varcal.cheermanager.Controller.Inventario;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.DTO.Inventario.ProductoDTO;
import com.varcal.cheermanager.DTO.Inventario.ProductoResponseDTO;
import com.varcal.cheermanager.Service.Inventario.ProductoService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    @GetMapping
    @RequiresPermission("ver_producto")
    public ResponseEntity<List<ProductoResponseDTO>> listar(
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(productoService.listarTodos(q));
    }

    @GetMapping("/activos")
    @RequiresPermission("ver_producto")
    public ResponseEntity<List<ProductoResponseDTO>> listarActivos() {
        return ResponseEntity.ok(productoService.listarActivos());
    }

    @GetMapping("/{id}")
    @RequiresPermission("ver_producto")
    public ResponseEntity<?> obtener(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(productoService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @RequiresPermission("crear_producto")
    public ResponseEntity<?> crear(@RequestBody ProductoDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @RequiresPermission("modificar_producto")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody ProductoDTO dto) {
        try {
            return ResponseEntity.ok(productoService.actualizar(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // No hay borrado físico — desactiva el producto (ver nota en el service).
    @DeleteMapping("/{id}")
    @RequiresPermission("eliminar_producto")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            productoService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}