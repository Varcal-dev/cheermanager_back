package com.varcal.cheermanager.Controller.Financiero;

import com.varcal.cheermanager.Utils.RequiresPermission;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.DTO.Financiero.TipoPlanPagoDTO;
import com.varcal.cheermanager.Models.Financiero.TipoPlanPago;
import com.varcal.cheermanager.Service.Financiero.TipoPlanPagoService;

import jakarta.persistence.EntityNotFoundException;

// CRUD del catálogo de tipos de plan de pago. Antes de esto, un tipo nuevo
// (ej. "Mensualidad Trimestral") solo se podía crear insertándolo
// directamente en la base de datos — la lista de tipos que usa el dropdown
// de PlanPagoModal (GET /api/planes-pago/tipos) sigue igual, este
// controller es el que faltaba para poder administrarlos.
@RestController
@RequestMapping("/api/tipos-planes-pago")
public class TipoPlanPagoController {

    @Autowired
    private TipoPlanPagoService service;

    @GetMapping
    @RequiresPermission("ver_plan_pago")
    public ResponseEntity<List<TipoPlanPagoDTO>> listar(
            @RequestParam(required = false) String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return ResponseEntity.ok(service.listarTodosDTO(null));
        }
        try {
            TipoPlanPago.Categoria cat = TipoPlanPago.Categoria.valueOf(categoria.toLowerCase());
            return ResponseEntity.ok(service.listarTodosDTO(cat));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    @RequiresPermission("ver_plan_pago")
    public ResponseEntity<?> obtener(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(service.obtenerPorId(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PostMapping
    @RequiresPermission("gestionar_plan_pago")
    public ResponseEntity<?> crear(@RequestBody TipoPlanPagoDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @RequiresPermission("gestionar_plan_pago")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody TipoPlanPagoDTO dto) {
        try {
            return ResponseEntity.ok(service.actualizar(id, dto));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("gestionar_plan_pago")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            service.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (IllegalStateException e) {
            // Tiene planes asociados: no se puede eliminar.
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }
}