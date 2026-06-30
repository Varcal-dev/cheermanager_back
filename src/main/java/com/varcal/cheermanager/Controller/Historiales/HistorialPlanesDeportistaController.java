package com.varcal.cheermanager.Controller.Historiales;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.DTO.Historiales.HistorialPlanesDeportistaDTO;
import com.varcal.cheermanager.Service.Historiales.HistorialPlanesDeportistaService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/historial-planes")
public class HistorialPlanesDeportistaController {

    @Autowired
    private HistorialPlanesDeportistaService historialPlanesDeportistaService;

    @PostMapping
    @RequiresPermission("modificar_inscripcion")
    public ResponseEntity<?> registrar(@RequestBody Map<String, Object> body) {
        try {
            Integer deportistaId = (Integer) body.get("deportistaId");
            Integer planPagoId = (Integer) body.get("planPagoId");
            String motivo = (String) body.get("motivoCambio");
            LocalDate fechaCambio = body.get("fechaCambio") != null
                    ? LocalDate.parse(body.get("fechaCambio").toString())
                    : null;

            return ResponseEntity.status(201).body(
                    historialPlanesDeportistaService.registrarCambioDePlan(deportistaId, planPagoId, fechaCambio, motivo));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/deportista/{deportistaId}")
    @RequiresPermission("ver_inscripcion")
    public ResponseEntity<List<HistorialPlanesDeportistaDTO>> listarPorDeportista(@PathVariable Integer deportistaId) {
        return ResponseEntity.ok(historialPlanesDeportistaService.listarPorDeportista(deportistaId));
    }
}