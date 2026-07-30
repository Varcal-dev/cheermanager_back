package com.varcal.cheermanager.Controller.Evaluaciones;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Models.Evaluaciones.EstadoObjetivo;
import com.varcal.cheermanager.Service.Evaluaciones.EstadoObjetivoService;
import com.varcal.cheermanager.Utils.RequiresPermission;

// Antes no existía ningún endpoint para esta tabla — el frontend no tenía
// forma de saber qué estados de objetivo existen para poblar un dropdown
// (Pendiente, En proceso, Cumplido, etc.), a pesar de que el repositorio
// (EstadoObjetivoRepository) ya estaba listo.
@RestController
@RequestMapping("/api/estados-objetivo")
public class EstadoObjetivoController {

    @Autowired
    private EstadoObjetivoService estadoObjetivoService;

    @GetMapping
    @RequiresPermission("ver_objetivos")
    public ResponseEntity<List<EstadoObjetivo>> listar() {
        return ResponseEntity.ok(estadoObjetivoService.obtenerTodos());
    }
}