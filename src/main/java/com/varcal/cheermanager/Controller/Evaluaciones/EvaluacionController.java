package com.varcal.cheermanager.Controller.Evaluaciones;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.DTO.Evaluaciones.EvaluacionDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.EvaluacionResponseDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.RegistroEvaluacionMasivoDTO;
import com.varcal.cheermanager.Service.Evaluaciones.EvaluacionService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/evaluaciones")
public class EvaluacionController {

    @Autowired
    private EvaluacionService evaluacionService;

    @PostMapping
    @RequiresPermission("crear_evaluacion")
    public ResponseEntity<?> registrar(@RequestBody EvaluacionDTO dto) {
        try {
            return ResponseEntity.status(201).body(evaluacionService.registrar(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // El caso de uso real: evaluar a un deportista en varios criterios de una
    // sola sesión. Devuelve 207 si algún criterio del lote falló mientras el
    // resto sí se guardó.
    @PostMapping("/masivo")
    @RequiresPermission("crear_evaluacion")
    public ResponseEntity<EvaluacionService.ResultadoRegistroMasivo> registrarMasivo(@RequestBody RegistroEvaluacionMasivoDTO dto) {
        EvaluacionService.ResultadoRegistroMasivo resultado = evaluacionService.registrarMasivo(dto);
        HttpStatus status = resultado.getErrores().isEmpty() ? HttpStatus.CREATED : HttpStatus.MULTI_STATUS;
        return ResponseEntity.status(status).body(resultado);
    }

    @GetMapping("/deportista/{deportistaId}")
    @RequiresPermission("ver_evaluaciones")
    public ResponseEntity<?> listarPorDeportista(@PathVariable Integer deportistaId) {
        try {
            return ResponseEntity.ok(evaluacionService.listarPorDeportista(deportistaId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/deportista/{deportistaId}/rango")
    @RequiresPermission("ver_evaluaciones")
    public ResponseEntity<List<EvaluacionResponseDTO>> listarPorRango(
            @PathVariable Integer deportistaId,
            @RequestParam LocalDate desde,
            @RequestParam LocalDate hasta) {
        return ResponseEntity.ok(evaluacionService.listarPorDeportistaYRango(deportistaId, desde, hasta));
    }

    // Para graficar la evolución de un deportista en un criterio específico.
    @GetMapping("/deportista/{deportistaId}/criterio/{criterioId}/progresion")
    @RequiresPermission("ver_evaluaciones")
    public ResponseEntity<List<EvaluacionResponseDTO>> progresionPorCriterio(
            @PathVariable Integer deportistaId, @PathVariable Integer criterioId) {
        return ResponseEntity.ok(evaluacionService.progresionPorCriterio(deportistaId, criterioId));
    }

    // Foto técnica actual: última evaluación en cada criterio de una categoría.
    @GetMapping("/deportista/{deportistaId}/categoria/{categoriaId}/actual")
    @RequiresPermission("ver_evaluaciones")
    public ResponseEntity<List<EvaluacionResponseDTO>> ultimaEvaluacionPorCategoria(
            @PathVariable Integer deportistaId, @PathVariable Integer categoriaId) {
        return ResponseEntity.ok(evaluacionService.ultimaEvaluacionPorCategoria(deportistaId, categoriaId));
    }

    @GetMapping("/deportista/{deportistaId}/criterio/{criterioId}/promedio")
    @RequiresPermission("ver_evaluaciones")
    public ResponseEntity<Map<String, Object>> promedioPorCriterio(
            @PathVariable Integer deportistaId, @PathVariable Integer criterioId) {
        Double promedio = evaluacionService.promedioPorCriterio(deportistaId, criterioId);
        return ResponseEntity.ok(Map.of("deportistaId", deportistaId, "criterioId", criterioId, "promedio", promedio));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("eliminar_evaluacion")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            evaluacionService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}