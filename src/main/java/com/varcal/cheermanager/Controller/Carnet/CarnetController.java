package com.varcal.cheermanager.Controller.Carnet;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.Service.Carnet.CarnetService;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** C13: carnet digital con QR y verificacion del codigo escaneado. */
@RestController
@RequestMapping("/api/carnet")
public class CarnetController {

    private final CarnetService servicio;

    public CarnetController(CarnetService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/deportista/{deportistaId}")
    @RequiresPermission("ver_deportista")
    public ResponseEntity<?> carnet(@PathVariable Integer deportistaId) {
        try {
            return ResponseEntity.ok(servicio.carnet(deportistaId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/verificar")
    @RequiresPermission("ver_deportista")
    public ResponseEntity<?> verificar(@RequestParam String codigo) {
        try {
            return ResponseEntity.ok(servicio.verificar(codigo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/entrenador/{entrenadorId}")
    @RequiresPermission("ver_entrenador")
    public ResponseEntity<?> carnetEntrenador(@PathVariable Integer entrenadorId) {
        try {
            return ResponseEntity.ok(servicio.carnetEntrenador(entrenadorId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
