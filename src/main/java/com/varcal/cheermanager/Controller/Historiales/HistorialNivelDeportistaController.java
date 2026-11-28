package com.varcal.cheermanager.Controller.Historiales;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.config.ErrorPublico;
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
    public record CambioNivelRequest(
            @jakarta.validation.constraints.NotNull(message = "Indica el deportista") Integer deportistaId,
            @jakarta.validation.constraints.NotNull(message = "El nivel es obligatorio") Integer nivelId,
            LocalDate fechaCambio,
            @jakarta.validation.constraints.Size(max = 300, message = "El motivo admite maximo 300 caracteres") String motivo) {
    }

    @PostMapping
    @RequiresPermission("modificar_deportista")
    public ResponseEntity<?> registrar(@jakarta.validation.Valid @RequestBody CambioNivelRequest body) {
        try {
            return ResponseEntity.status(201).body(historialNivelDeportistaService.registrarCambio(
                    body.deportistaId(), body.nivelId(), body.fechaCambio(), body.motivo()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ErrorPublico.de(e));
        }
    }

    @GetMapping("/deportista/{deportistaId}")
    @RequiresPermission("ver_deportistas")
    public ResponseEntity<List<HistorialNivelDeportistaDTO>> listarPorDeportista(@PathVariable Integer deportistaId) {
        return ResponseEntity.ok(historialNivelDeportistaService.listarPorDeportista(deportistaId));
    }
}