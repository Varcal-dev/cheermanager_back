package com.varcal.cheermanager.Controller.Org_dep;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.Models.Org_dep.Temporada;
import com.varcal.cheermanager.Service.Org_dep.TemporadaService;
import com.varcal.cheermanager.Service.Org_dep.TemporadaService.TemporadaRequest;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/temporadas")
public class TemporadaController {

    private final TemporadaService service;

    public TemporadaController(TemporadaService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission("ver_temporadas")
    public List<Temporada> listar() {
        return service.listar();
    }

    @GetMapping("/activa")
    @RequiresPermission("ver_temporadas")
    public ResponseEntity<Temporada> activa() {
        return service.activa().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping
    @RequiresPermission("gestionar_temporadas")
    public ResponseEntity<Temporada> crear(@RequestBody TemporadaRequest req) {
        return ResponseEntity.status(201).body(service.crear(req));
    }

    @PutMapping("/{id}")
    @RequiresPermission("gestionar_temporadas")
    public Temporada actualizar(@PathVariable Integer id, @RequestBody TemporadaRequest req) {
        return service.actualizar(id, req);
    }

    @PatchMapping("/{id}/activar")
    @RequiresPermission("gestionar_temporadas")
    public Temporada activar(@PathVariable Integer id) {
        return service.activar(id);
    }

    @PatchMapping("/{id}/cerrar")
    @RequiresPermission("gestionar_temporadas")
    public Temporada cerrar(@PathVariable Integer id) {
        return service.cerrar(id);
    }
}