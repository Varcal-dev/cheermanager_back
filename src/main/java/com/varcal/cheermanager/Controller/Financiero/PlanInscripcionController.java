package com.varcal.cheermanager.Controller.Financiero;

import com.varcal.cheermanager.Models.Financiero.PlanInscripcion;
import com.varcal.cheermanager.Service.Financiero.PlanInscripcionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/planes-inscripcion")
public class PlanInscripcionController {

    @Autowired
    private PlanInscripcionService planInscripcionService;

    @GetMapping("/listar")
    public ResponseEntity<List<PlanInscripcion>> listarPlanes() {
        return ResponseEntity.ok(planInscripcionService.listarPlanes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(planInscripcionService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
