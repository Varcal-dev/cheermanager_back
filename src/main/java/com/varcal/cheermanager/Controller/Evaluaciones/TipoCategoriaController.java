package com.varcal.cheermanager.Controller.Evaluaciones;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Utils.RequiresPermission;
import com.varcal.cheermanager.repository.Evaluaciones.TipoCategoriaRepository;

// Catalogo global (Fisica, Tecnica, ...) de solo lectura: alimenta el selector de categoria de los criterios.
@RestController
@RequestMapping("/api/tipos-categoria")
public class TipoCategoriaController {

    @Autowired
    private TipoCategoriaRepository tipoCategoriaRepository;

    @GetMapping
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<List<Map<String, Object>>> listar() {
        return ResponseEntity.ok(tipoCategoriaRepository.findAll().stream()
                .<Map<String, Object>>map(t -> Map.of("id", t.getId(), "categoria", t.getCategoria()))
                .toList());
    }
}