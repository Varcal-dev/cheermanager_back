package com.varcal.cheermanager.Controller.Evaluaciones;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Service.Evaluaciones.EvaluacionPlantillaService;
import com.varcal.cheermanager.Service.Evaluaciones.EvaluacionPlantillaService.PlantillaEntrada;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** Plantillas de evaluacion tipo tryout (criterios de la rubrica y de habilidades). */
@RestController
@RequestMapping("/api/evaluacion-plantillas")
public class EvaluacionPlantillaController {

    @Autowired
    private EvaluacionPlantillaService service;

    @GetMapping("/catalogo")
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<?> catalogo(@RequestParam(required = false) String modalidad) {
        try {
            return ResponseEntity.ok(service.catalogo(modalidad));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @GetMapping
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<?> obtener(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(service.obtener(id));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @PostMapping
    @RequiresPermission("crear_evaluacion")
    public ResponseEntity<?> crear(@RequestBody PlantillaEntrada body) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(body));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @PutMapping("/{id}")
    @RequiresPermission("modificar_evaluacion")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody PlantillaEntrada body) {
        try {
            return ResponseEntity.ok(service.actualizar(id, body));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("eliminar_evaluacion")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            service.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @GetMapping("/{id}/fechas")
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<?> fechas(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(service.fechas(id));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @GetMapping("/{id}/resultados")
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<?> resultados(@PathVariable Integer id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        try {
            return ResponseEntity.ok(service.resultados(id, fecha));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    private ResponseEntity<?> error(RuntimeException e) {
        HttpStatus s = e instanceof IllegalArgumentException ? HttpStatus.BAD_REQUEST
                : e instanceof IllegalStateException ? HttpStatus.CONFLICT : HttpStatus.NOT_FOUND;
        return ResponseEntity.status(s).body(java.util.Map.of("message", String.valueOf(e.getMessage())));
    }
}
