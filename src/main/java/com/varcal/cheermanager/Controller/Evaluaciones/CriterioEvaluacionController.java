package com.varcal.cheermanager.Controller.Evaluaciones;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.DTO.Evaluaciones.CriterioEvaluacionResponseDTO;
import com.varcal.cheermanager.Service.Evaluaciones.CriterioEvaluacionService;
import com.varcal.cheermanager.Utils.RequiresPermission;

// Este archivo estaba vacío (solo el package + la declaración de la clase,
// sin @RequestMapping ni un solo endpoint) — el frontend no tenía forma de
// listar qué criterios de evaluación existen para poblar el formulario de
// "Nueva evaluación".
@RestController
@RequestMapping("/api/criterios-evaluacion")
public class CriterioEvaluacionController {

    @Autowired
    private CriterioEvaluacionService criterioEvaluacionService;

    @GetMapping
    @RequiresPermission("ver_evaluaciones")
    public ResponseEntity<List<CriterioEvaluacionResponseDTO>> listar() {
        return ResponseEntity.ok(criterioEvaluacionService.listar());
    }

    @GetMapping("/categoria/{categoriaId}")
    @RequiresPermission("ver_evaluaciones")
    public ResponseEntity<List<CriterioEvaluacionResponseDTO>> listarPorCategoria(
            @PathVariable Integer categoriaId) {
        return ResponseEntity.ok(criterioEvaluacionService.listarPorCategoria(categoriaId));
    }
}