package com.varcal.cheermanager.Controller.Inventario;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.DTO.Inventario.MovimientoInventarioDTO;
import com.varcal.cheermanager.DTO.Inventario.MovimientoInventarioResponseDTO;
import com.varcal.cheermanager.Service.Inventario.MovimientoInventarioService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/movimientos-inventario")
public class MovimientoInventarioController {

    @Autowired
    private MovimientoInventarioService movimientoInventarioService;

    @PostMapping
    @RequiresPermission("registrar_movimiento_inventario")
    public ResponseEntity<?> registrar(@RequestBody MovimientoInventarioDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(movimientoInventarioService.registrar(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @GetMapping("/variante/{productoVarianteId}")
    @RequiresPermission("ver_movimiento_inventario")
    public ResponseEntity<List<MovimientoInventarioResponseDTO>> listarPorVariante(@PathVariable Integer productoVarianteId) {
        return ResponseEntity.ok(movimientoInventarioService.listarPorVariante(productoVarianteId));
    }
}