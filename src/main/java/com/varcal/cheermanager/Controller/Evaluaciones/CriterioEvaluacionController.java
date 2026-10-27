package com.varcal.cheermanager.Controller.Evaluaciones;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.DTO.Evaluaciones.CriterioEvaluacionDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.CriterioEvaluacionResponseDTO;
import com.varcal.cheermanager.Service.Evaluaciones.CriterioEvaluacionService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/criterios-evaluacion")
public class CriterioEvaluacionController {

    @Autowired
    private CriterioEvaluacionService criterioEvaluacionService;

    @GetMapping
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<List<CriterioEvaluacionResponseDTO>> listar() {
        return ResponseEntity.ok(criterioEvaluacionService.listar());
    }

    @GetMapping("/categoria/{categoriaId}")
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<List<CriterioEvaluacionResponseDTO>> listarPorCategoria(
            @PathVariable Integer categoriaId) {
        return ResponseEntity.ok(criterioEvaluacionService.listarPorCategoria(categoriaId));
    }

    @PostMapping
    @RequiresPermission("crear_criterio_evaluacion")
    public ResponseEntity<?> crear(@RequestBody CriterioEvaluacionDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(criterioEvaluacionService.crear(dto));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @PutMapping("/{id}")
    @RequiresPermission("modificar_criterio_evaluacion")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody CriterioEvaluacionDTO dto) {
        try {
            return ResponseEntity.ok(criterioEvaluacionService.actualizar(id, dto));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("eliminar_criterio_evaluacion")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            criterioEvaluacionService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    // 400 dato invalido, 409 regla/datos existentes, 404 no encontrado.
    private ResponseEntity<String> error(RuntimeException e) {
        HttpStatus status = e instanceof IllegalArgumentException ? HttpStatus.BAD_REQUEST
                : e instanceof IllegalStateException ? HttpStatus.CONFLICT
                : HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(e.getMessage());
    }
}