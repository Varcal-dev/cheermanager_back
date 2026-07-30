package com.varcal.cheermanager.Controller.Org_dep;

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

import com.varcal.cheermanager.Models.Org_dep.EstadoCategoriaNivel;
import com.varcal.cheermanager.Service.Org_dep.EstadoCategoriaNivelService;

@RestController
@RequestMapping("/api/estados-categoria-nivel")
public class EstadoCategoriaNivelController {

    @Autowired
    private EstadoCategoriaNivelService estadoCategoriaNivelService;

    @GetMapping
    public List<EstadoCategoriaNivel> listarTodos() {
        return estadoCategoriaNivelService.obtenerTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(estadoCategoriaNivelService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<EstadoCategoriaNivel> crear(@RequestBody EstadoCategoriaNivel estado) {
        return ResponseEntity.status(HttpStatus.CREATED).body(estadoCategoriaNivelService.crear(estado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody EstadoCategoriaNivel estado) {
        try {
            return ResponseEntity.ok(estadoCategoriaNivelService.actualizar(id, estado));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            estadoCategoriaNivelService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}