package com.varcal.cheermanager.Controller.Evaluaciones.Rutina;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.DTO.Evaluaciones.Rutina.EvaluacionRutinaDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.Rutina.EvaluacionRutinaResponseDTO;
import com.varcal.cheermanager.Service.Evaluaciones.Rutina.EvaluacionRutinaService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/evaluaciones-rutina")
public class EvaluacionRutinaController {

    @Autowired
    private EvaluacionRutinaService evaluacionRutinaService;

    // El juez ingresa los datos crudos de todos los sub-criterios en un solo
    // request. El service calcula automáticamente cada sub-puntaje y el total.
    @PostMapping
    @RequiresPermission("crear_evaluacion_rutina")
    public ResponseEntity<?> registrar(@RequestBody EvaluacionRutinaDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(evaluacionRutinaService.registrar(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @GetMapping("/grupo/{grupoId}")
    @RequiresPermission("ver_evaluaciones")
    public ResponseEntity<List<EvaluacionRutinaResponseDTO>> listarPorGrupo(@PathVariable Integer grupoId) {
        return ResponseEntity.ok(evaluacionRutinaService.listarPorGrupo(grupoId));
    }

    @GetMapping("/{id}")
    @RequiresPermission("ver_evaluaciones")
    public ResponseEntity<?> obtener(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(evaluacionRutinaService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("eliminar_evaluacion")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            evaluacionRutinaService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}