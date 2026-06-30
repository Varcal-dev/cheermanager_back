package com.varcal.cheermanager.Controller.Historiales;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.DTO.Historiales.HistorialNivelDeportistaDTO;
import com.varcal.cheermanager.Service.Historiales.HistorialNivelDeportistaService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/historial-nivel")
public class HistorialNivelDeportistaController {

    @Autowired
    private HistorialNivelDeportistaService historialNivelDeportistaService;

    // Registro manual. Si en el futuro PersonaService.modificarDeportista llama
    // a este mismo service internamente al detectar un cambio de nivelActualId,
    // este endpoint sigue funcionando igual para correcciones o registros
    // retroactivos (ej. un cambio de nivel que pasó hace dos semanas y no se
    // alcanzó a registrar en su momento).
    @PostMapping
    @RequiresPermission("modificar_deportista")
    public ResponseEntity<?> registrar(@RequestBody Map<String, Object> body) {
        try {
            Integer deportistaId = (Integer) body.get("deportistaId");
            Integer nivelId = (Integer) body.get("nivelId");
            String motivo = (String) body.get("motivo");
            LocalDate fechaCambio = body.get("fechaCambio") != null
                    ? LocalDate.parse(body.get("fechaCambio").toString())
                    : null;

            return ResponseEntity.status(201).body(
                    historialNivelDeportistaService.registrarCambio(deportistaId, nivelId, fechaCambio, motivo));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/deportista/{deportistaId}")
    @RequiresPermission("ver_deportistas")
    public ResponseEntity<List<HistorialNivelDeportistaDTO>> listarPorDeportista(@PathVariable Integer deportistaId) {
        return ResponseEntity.ok(historialNivelDeportistaService.listarPorDeportista(deportistaId));
    }
}