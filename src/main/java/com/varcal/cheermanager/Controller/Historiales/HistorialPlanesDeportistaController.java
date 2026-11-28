package com.varcal.cheermanager.Controller.Historiales;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.config.ErrorPublico;
import com.varcal.cheermanager.DTO.Historiales.HistorialPlanesDeportistaDTO;
import com.varcal.cheermanager.Service.Historiales.HistorialPlanesDeportistaService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/historial-planes")
public class HistorialPlanesDeportistaController {

    @Autowired
    private HistorialPlanesDeportistaService historialPlanesDeportistaService;

    public record CambioPlanRequest(
            @jakarta.validation.constraints.NotNull(message = "Indica el deportista") Integer deportistaId,
            @jakarta.validation.constraints.NotNull(message = "Indica el plan de pago") Integer planPagoId,
            LocalDate fechaCambio,
            @jakarta.validation.constraints.Size(max = 255, message = "El motivo admite maximo 255 caracteres") String motivoCambio) {
    }

    @PostMapping
    @RequiresPermission("modificar_inscripcion")
    public ResponseEntity<?> registrar(@jakarta.validation.Valid @RequestBody CambioPlanRequest body) {
        try {
            return ResponseEntity.status(201).body(historialPlanesDeportistaService.registrarCambioDePlan(
                    body.deportistaId(), body.planPagoId(), body.fechaCambio(), body.motivoCambio()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ErrorPublico.de(e));
        }
    }

    @GetMapping("/deportista/{deportistaId}")
    @RequiresPermission("ver_inscripcion")
    public ResponseEntity<List<HistorialPlanesDeportistaDTO>> listarPorDeportista(@PathVariable Integer deportistaId) {
        return ResponseEntity.ok(historialPlanesDeportistaService.listarPorDeportista(deportistaId));
    }
}