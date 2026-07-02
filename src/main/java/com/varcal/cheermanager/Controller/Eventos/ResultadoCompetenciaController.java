package com.varcal.cheermanager.Controller.Eventos;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.DTO.Eventos.ResultadoCompetenciaDTO;
import com.varcal.cheermanager.DTO.Eventos.ResultadoCompetenciaResponseDTO;
import com.varcal.cheermanager.Service.Eventos.ResultadoCompetenciaService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/resultados-competencia")
public class ResultadoCompetenciaController {

    @Autowired
    private ResultadoCompetenciaService resultadoCompetenciaService;

    @PostMapping
    @RequiresPermission("modificar_evento")
    public ResponseEntity<?> registrar(@RequestBody ResultadoCompetenciaDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(resultadoCompetenciaService.registrar(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // Tabla de posiciones de un evento, ya ordenada por puesto.
    @GetMapping("/evento/{eventoId}")
    @RequiresPermission("ver_evento")
    public ResponseEntity<?> listarPorEvento(@PathVariable Integer eventoId) {
        try {
            return ResponseEntity.ok(resultadoCompetenciaService.listarPorEvento(eventoId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Línea de tiempo de resultados de un grupo — la fuente del KPI de
    // progresión de puntaje evento-a-evento.
    @GetMapping("/grupo/{grupoId}")
    @RequiresPermission("ver_evento")
    public ResponseEntity<?> listarPorGrupo(@PathVariable Integer grupoId) {
        try {
            return ResponseEntity.ok(resultadoCompetenciaService.listarPorGrupo(grupoId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    @RequiresPermission("modificar_evento")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody ResultadoCompetenciaDTO dto) {
        try {
            return ResponseEntity.ok(resultadoCompetenciaService.actualizar(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("modificar_evento")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            resultadoCompetenciaService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}