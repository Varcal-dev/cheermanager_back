package com.varcal.cheermanager.Controller.Evaluaciones;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.DTO.Evaluaciones.ObjetivoDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.ObjetivoResponseDTO;
import com.varcal.cheermanager.Service.Evaluaciones.ObjetivoService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/objetivos")
public class ObjetivoController {

    @Autowired
    private ObjetivoService objetivoService;

    @PostMapping
    @RequiresPermission("crear_objetivo")
    public ResponseEntity<?> crear(@RequestBody ObjetivoDTO dto) {
        try {
            return ResponseEntity.status(201).body(objetivoService.crear(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/deportista/{deportistaId}")
    @RequiresPermission("ver_objetivos")
    public ResponseEntity<?> listarPorDeportista(@PathVariable Integer deportistaId) {
        try {
            return ResponseEntity.ok(objetivoService.listarPorDeportista(deportistaId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/deportista/{deportistaId}/estado/{estadoId}")
    @RequiresPermission("ver_objetivos")
    public ResponseEntity<List<ObjetivoResponseDTO>> listarPorDeportistaYEstado(
            @PathVariable Integer deportistaId, @PathVariable Integer estadoId) {
        return ResponseEntity.ok(objetivoService.listarPorDeportistaYEstado(deportistaId, estadoId));
    }

    // Panel del entrenador: todos los objetivos en un estado dado (ej. "En
    // progreso") sin filtrar por deportista.
    @GetMapping("/estado/{estadoId}")
    @RequiresPermission("ver_objetivos")
    public ResponseEntity<List<ObjetivoResponseDTO>> listarPorEstado(@PathVariable Integer estadoId) {
        return ResponseEntity.ok(objetivoService.listarPorEstado(estadoId));
    }

    @PutMapping("/{id}")
    @RequiresPermission("modificar_objetivo")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody ObjetivoDTO dto) {
        try {
            return ResponseEntity.ok(objetivoService.actualizar(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/estado")
    @RequiresPermission("modificar_objetivo")
    public ResponseEntity<?> cambiarEstado(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        try {
            Integer nuevoEstadoId = body.get("estadoObjetivoId");
            return ResponseEntity.ok(objetivoService.cambiarEstado(id, nuevoEstadoId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("eliminar_objetivo")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            objetivoService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}