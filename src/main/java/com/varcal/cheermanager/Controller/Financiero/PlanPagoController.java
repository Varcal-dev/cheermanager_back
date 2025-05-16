package com.varcal.cheermanager.Controller.Financiero;

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
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.DTO.Financiero.PlanPagoRequestDTO;
import com.varcal.cheermanager.DTO.Financiero.PlanPagoResponseDTO;
import com.varcal.cheermanager.Service.Financiero.PlanPagoService;

@RestController
@RequestMapping("/api/planes-pago")
public class PlanPagoController {

    private final PlanPagoService planPagoService;

    @Autowired
    public PlanPagoController(PlanPagoService planPagoService) {
        this.planPagoService = planPagoService;
    }

    @GetMapping
    public List<PlanPagoResponseDTO> listarTodos() {
        return planPagoService.listarPlanes();
    }

    @PostMapping
    public ResponseEntity<PlanPagoResponseDTO> crear(@RequestBody PlanPagoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planPagoService.crearPlan(dto));
    }

     // Obtener un plan por ID
    @GetMapping("/{id}")
    public ResponseEntity<PlanPagoResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(planPagoService.obtenerPorId(id));
    }

    // Actualizar plan de pago
    @PutMapping("/{id}")
    public ResponseEntity<PlanPagoResponseDTO> actualizar(
            @PathVariable Integer id,
            @RequestBody PlanPagoRequestDTO dto) {
        return ResponseEntity.ok(planPagoService.actualizarPlan(id, dto));
    }

    // Eliminar plan de pago
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        planPagoService.eliminarPlan(id);
        return ResponseEntity.noContent().build();
    }
}
