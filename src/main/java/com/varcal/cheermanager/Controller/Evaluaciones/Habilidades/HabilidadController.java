package com.varcal.cheermanager.Controller.Evaluaciones.Habilidades;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.Service.Evaluaciones.Habilidades.EvaluacionHabilidadService;
import com.varcal.cheermanager.Utils.AuthenticatedUserUtil;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** C1: glosario de habilidades N1-N4 y su evaluacion por drivers. */
@RestController
@RequestMapping("/api/habilidades")
public class HabilidadController {

    private final EvaluacionHabilidadService servicio;

    public HabilidadController(EvaluacionHabilidadService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @RequiresPermission("ver_evaluaciones")
    public ResponseEntity<?> catalogo(@RequestParam(required = false) String nivel,
            @RequestParam(required = false) String categoria, @RequestParam(required = false) String estilo) {
        return ResponseEntity.ok(servicio.listarCatalogo(nivel, categoria, estilo));
    }

    @GetMapping("/drivers/{categoria}")
    @RequiresPermission("ver_evaluaciones")
    public ResponseEntity<?> drivers(@PathVariable String categoria) {
        try {
            return ResponseEntity.ok(servicio.driversDe(categoria));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/deportista/{deportistaId}")
    @RequiresPermission("ver_evaluaciones")
    public ResponseEntity<?> estado(@PathVariable Integer deportistaId) {
        try {
            return ResponseEntity.ok(servicio.estadoDeDeportista(deportistaId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/deportista/{deportistaId}/{habilidadId}/historial")
    @RequiresPermission("ver_evaluaciones")
    public ResponseEntity<?> historial(@PathVariable Integer deportistaId, @PathVariable Integer habilidadId) {
        return ResponseEntity.ok(servicio.historial(deportistaId, habilidadId));
    }

    @PostMapping("/evaluar")
    @RequiresPermission("registrar_evaluacion")
    public ResponseEntity<?> evaluar(@RequestBody EvaluacionHabilidadService.Solicitud solicitud) {
        try {
            return ResponseEntity.status(201)
                    .body(servicio.evaluar(solicitud, AuthenticatedUserUtil.getCurrentUserId().intValue()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        }
    }
}
