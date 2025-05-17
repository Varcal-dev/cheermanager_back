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

import com.varcal.cheermanager.DTO.Financiero.PlanPagoDTO;
import com.varcal.cheermanager.DTO.Financiero.PlanPagoResponseDTO;
import com.varcal.cheermanager.Models.Financiero.PlanPago;
import com.varcal.cheermanager.Models.Financiero.TipoPlanPago;
import com.varcal.cheermanager.Service.Financiero.PlanPagoService;
import com.varcal.cheermanager.Service.Financiero.TipoPlanPagoService;
import com.varcal.cheermanager.repository.Financiero.PlanPagoRepository;
import com.varcal.cheermanager.repository.Financiero.TipoPlanPagoRepository;

@RestController
@RequestMapping("/api/planes-pago")
public class PlanPagoController {

    private final PlanPagoService planPagoService;

    @Autowired
    private TipoPlanPagoService service;

    @Autowired
    private PlanPagoRepository planPagoRepository;

    @Autowired
    private TipoPlanPagoRepository tipoPlanPagoRepository;

    @Autowired
    public PlanPagoController(PlanPagoService planPagoService) {
        this.planPagoService = planPagoService;
    }

    @GetMapping
    public List<PlanPagoResponseDTO> listarTodos() {
        return planPagoService.listarPlanes();
    }

    @PostMapping
public ResponseEntity<PlanPago> crearPlan(@RequestBody PlanPagoDTO dto) {
    TipoPlanPago tipo = tipoPlanPagoRepository.findById(dto.getTipoPlan())
        .orElseThrow(() -> new RuntimeException("Tipo de plan no encontrado"));

    PlanPago plan = new PlanPago();
    plan.setTipoPlan(tipo);
    plan.setDescripcion(dto.getDescripcion());
    plan.setValorMensual(dto.getValorMensual());
    plan.setFechaVigenciaInicio(dto.getFechaVigenciaInicio());
    plan.setFechaVigenciaFin(dto.getFechaVigenciaFin());
    plan.setActivo(dto.getActivo());

    PlanPago creado = planPagoRepository.save(plan);
    return ResponseEntity.status(HttpStatus.CREATED).body(creado);
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
            @RequestBody PlanPagoResponseDTO dto) {
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
