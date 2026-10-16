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

import com.varcal.cheermanager.DTO.Financiero.PlanPagoDTO;
import com.varcal.cheermanager.Models.Financiero.TipoPlanPago;
import com.varcal.cheermanager.Service.Financiero.PlanPagoService;
import com.varcal.cheermanager.Service.Financiero.TipoPlanPagoService;

@RestController
@RequestMapping("/api/planes-pago")
public class PlanPagoController {

    private final PlanPagoService planPagoService;

    @Autowired
    private TipoPlanPagoService tipoPlanPagoService;

    @Autowired
    public PlanPagoController(PlanPagoService planPagoService) {
        this.planPagoService = planPagoService;
    }

    // ?categoria=inscripcion | mensualidad (opcional). Es lo que usan
    // /planes-inscripcion y /planes-mensualidad en el front para mostrar
    // cada uno su propia vista del MISMO catálogo (planes_pago), en vez de
    // que /planes-inscripcion dependiera de la tabla huérfana
    // planes_inscripcion (retirada — no tenía ninguna FK real).
    @GetMapping
    @RequiresPermission("ver_plan_pago")
    public ResponseEntity<List<PlanPagoDTO>> listarTodos(
            @RequestParam(required = false) String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return ResponseEntity.ok(planPagoService.listarPlanes());
        }
        try {
            TipoPlanPago.Categoria cat = TipoPlanPago.Categoria.valueOf(categoria.toLowerCase());
            return ResponseEntity.ok(planPagoService.listarPlanesPorCategoria(cat));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    @RequiresPermission("gestionar_plan_pago")
    public ResponseEntity<?> crearPlan(@RequestBody PlanPagoDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(planPagoService.crearPlan(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @RequiresPermission("ver_plan_pago")
    public ResponseEntity<PlanPagoDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(planPagoService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    @RequiresPermission("gestionar_plan_pago")
    public ResponseEntity<?> actualizar(
            @PathVariable Integer id,
            @RequestBody PlanPagoDTO dto) {
        try {
            return ResponseEntity.ok(planPagoService.actualizarPlan(id, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("gestionar_plan_pago")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        planPagoService.eliminarPlan(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tipos")
    @RequiresPermission("ver_plan_pago")
    public ResponseEntity<?> listarTipos(@RequestParam(required = false) String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return ResponseEntity.ok(tipoPlanPagoService.listarTodos());
        }
        try {
            TipoPlanPago.Categoria cat = TipoPlanPago.Categoria.valueOf(categoria.toLowerCase());
            return ResponseEntity.ok(tipoPlanPagoService.listarPorCategoria(cat));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
