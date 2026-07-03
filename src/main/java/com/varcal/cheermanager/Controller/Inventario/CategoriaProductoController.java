package com.varcal.cheermanager.Controller.Inventario;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.DTO.Inventario.CategoriaProductoDTO;
import com.varcal.cheermanager.Models.Inventario.CategoriaProducto;
import com.varcal.cheermanager.Service.Inventario.CategoriaProductoService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/categorias-producto")
public class CategoriaProductoController {

    @Autowired
    private CategoriaProductoService categoriaProductoService;

    @GetMapping
    @RequiresPermission("ver_producto")
    public ResponseEntity<List<CategoriaProducto>> listar() {
        return ResponseEntity.ok(categoriaProductoService.listarTodas());
    }

    @GetMapping("/{id}")
    @RequiresPermission("ver_producto")
    public ResponseEntity<?> obtener(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(categoriaProductoService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @RequiresPermission("crear_producto")
    public ResponseEntity<?> crear(@RequestBody CategoriaProductoDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(categoriaProductoService.crear(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @RequiresPermission("modificar_producto")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody CategoriaProductoDTO dto) {
        try {
            return ResponseEntity.ok(categoriaProductoService.actualizar(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("eliminar_producto")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            categoriaProductoService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}