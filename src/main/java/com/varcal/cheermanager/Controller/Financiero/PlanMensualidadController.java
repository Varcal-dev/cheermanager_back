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

import com.varcal.cheermanager.DTO.Financiero.PlanMensualidadDTO;
import com.varcal.cheermanager.DTO.Financiero.PlanMensualidadResponseDTO;
import com.varcal.cheermanager.Models.Financiero.PlanMensualidad;
import com.varcal.cheermanager.Models.Financiero.TipoPlanPago;
import com.varcal.cheermanager.Service.Financiero.PlanMensualidadService;
import com.varcal.cheermanager.Service.Financiero.TipoPlanPagoService;
import com.varcal.cheermanager.repository.Financiero.PlanMensualidadRepository;
import com.varcal.cheermanager.repository.Financiero.TipoPlanPagoRepository;

@RestController
@RequestMapping("/api/planes-mensualidad")
public class PlanMensualidadController {

    private final PlanMensualidadService planPagoService;

    @Autowired
    private TipoPlanPagoService service;

    @Autowired
    private PlanMensualidadRepository planPagoRepository;

    @Autowired
    private TipoPlanPagoRepository tipoPlanPagoRepository;

    @Autowired
    public PlanMensualidadController(PlanMensualidadService planPagoService) {
        this.planPagoService = planPagoService;
    }

    @GetMapping
    public List<PlanMensualidadResponseDTO> listarTodos() {
        return planPagoService.listarPlanes();
    }

    @PostMapping
public ResponseEntity<PlanMensualidad> crearPlan(@RequestBody PlanMensualidadDTO dto) {
    Integer tipoPlanId = Integer.valueOf(dto.getTipoPlan());
    TipoPlanPago tipo = tipoPlanPagoRepository.findById(tipoPlanId)
        .orElseThrow(() -> new RuntimeException("Tipo de plan no encontrado"));

    PlanMensualidad plan = new PlanMensualidad();
    plan.setTipoPlan(tipo);
    plan.setDescripcion(dto.getDescripcion());
    plan.setValorMensual(dto.getValorMensual());
    plan.setFechaVigenciaInicio(dto.getFechaVigenciaInicio());
    plan.setFechaVigenciaFin(dto.getFechaVigenciaFin());
    plan.setActivo(dto.getActivo());

    PlanMensualidad creado = planPagoRepository.save(plan);
    return ResponseEntity.status(HttpStatus.CREATED).body(creado);
}


    // Obtener un plan por ID
    @GetMapping("/{id}")
    public ResponseEntity<PlanMensualidadResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(planPagoService.obtenerPorId(id));
    }

    // Actualizar plan de pago
    @PutMapping("/{id}")
    public ResponseEntity<PlanMensualidadResponseDTO> actualizar(
            @PathVariable Integer id,
            @RequestBody PlanMensualidadResponseDTO dto) {
        return ResponseEntity.ok(planPagoService.actualizarPlan(id, dto));
    }

    // Eliminar plan de pago
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        planPagoService.eliminarPlan(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tipos")
    public ResponseEntity<List<TipoPlanPago>> listar() {
        List<TipoPlanPago> tipos = service.listarTodos();
        return ResponseEntity.ok(tipos);
    }
}
