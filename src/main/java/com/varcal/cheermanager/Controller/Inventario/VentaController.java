package com.varcal.cheermanager.Controller.Inventario;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.DTO.Inventario.VentaDTO;
import com.varcal.cheermanager.DTO.Inventario.VentaResponseDTO;
import com.varcal.cheermanager.Service.Inventario.VentaService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    @Autowired
    private VentaService ventaService;

    @PostMapping
    @RequiresPermission("registrar_venta")
    public ResponseEntity<?> registrar(@RequestBody VentaDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.registrar(dto));
        } catch (RuntimeException e) {
            // 409 cubre tanto "comprador inválido" como "stock insuficiente"
            // (ambas son conflictos de reglas de negocio, no recursos faltantes).
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @RequiresPermission("ver_venta")
    public ResponseEntity<?> obtener(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(ventaService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    @RequiresPermission("ver_venta")
    public ResponseEntity<List<VentaResponseDTO>> listarPorRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(ventaService.listarPorRango(desde, hasta));
    }

    public record AnularRequest(String motivo) {
    }

    /** Reversa la venta: stock, pago y factura propia. No se borra nada; queda con motivo, fecha y usuario. */
    @PatchMapping("/{id}/anular")
    @RequiresPermission("anular_venta")
    public ResponseEntity<?> anular(@PathVariable Integer id, @RequestBody AnularRequest req) {
        try {
            return ResponseEntity.ok(ventaService.anular(id, req == null ? null : req.motivo()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    @GetMapping("/persona/{personaId}")
    @RequiresPermission("ver_venta")
    public ResponseEntity<List<VentaResponseDTO>> listarPorPersona(@PathVariable Integer personaId) {
        return ResponseEntity.ok(ventaService.listarPorPersona(personaId));
    }
}