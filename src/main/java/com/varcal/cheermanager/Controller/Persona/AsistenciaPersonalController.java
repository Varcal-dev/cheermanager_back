package com.varcal.cheermanager.Controller.Persona;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.Service.Persona.AsistenciaPersonalService;
import com.varcal.cheermanager.Utils.RequiresPermission;

/**
 * Asistencia del personal. Marcar con el carnet y ver el dia: registrar_asistencia. Reporte por rango: ver_entrenador.
 * Correccion manual y borrado: modificar_entrenador.
 */
@RestController
@RequestMapping("/api/asistencia-personal")
public class AsistenciaPersonalController {

    public record Escaneo(String codigo) {
    }

    @Autowired
    private AsistenciaPersonalService service;

    @PostMapping("/escaneo")
    @RequiresPermission("registrar_asistencia")
    public ResponseEntity<?> escanear(@RequestBody Escaneo body) {
        try {
            return ResponseEntity.ok(service.escanear(body.codigo()));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @GetMapping("/hoy")
    @RequiresPermission("registrar_asistencia")
    public ResponseEntity<?> hoy() {
        return ResponseEntity.ok(service.deHoy());
    }

    @GetMapping
    @RequiresPermission("ver_entrenador")
    public ResponseEntity<?> reporte(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer entrenadorId) {
        return ResponseEntity.ok(service.reporte(desde, hasta, entrenadorId));
    }

    @PostMapping("/manual")
    @RequiresPermission("modificar_entrenador")
    public ResponseEntity<?> manual(@RequestBody AsistenciaPersonalService.Manual body) {
        try {
            return ResponseEntity.ok(service.guardarManual(body));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("modificar_entrenador")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            service.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    // 400 dato invalido, 409 regla (p. ej. carnet no vigente), 404 no encontrado. Misma forma {message} que /asistencias/escaneo.
    private ResponseEntity<?> error(RuntimeException e) {
        HttpStatus s = e instanceof IllegalArgumentException ? HttpStatus.BAD_REQUEST
                : e instanceof IllegalStateException ? HttpStatus.CONFLICT : HttpStatus.NOT_FOUND;
        return ResponseEntity.status(s).body(Map.of("message", String.valueOf(e.getMessage())));
    }
}
