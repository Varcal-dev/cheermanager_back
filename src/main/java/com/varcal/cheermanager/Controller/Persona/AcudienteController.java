package com.varcal.cheermanager.Controller.Persona;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.Models.Personas.Acudiente;
import com.varcal.cheermanager.Service.Persona.AcudienteService;
import com.varcal.cheermanager.Service.Persona.AcudienteService.AcudienteRequest;
import com.varcal.cheermanager.Service.Persona.AcudienteService.VinculoRequest;
import com.varcal.cheermanager.Service.Persona.AcudienteService.VinculoResponse;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/acudientes")
public class AcudienteController {

    private final AcudienteService service;

    public AcudienteController(AcudienteService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission("ver_acudientes")
    public List<Acudiente> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @RequiresPermission("ver_acudientes")
    public Acudiente obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    @PostMapping
    @RequiresPermission("gestionar_acudientes")
    public ResponseEntity<Acudiente> crear(@RequestBody AcudienteRequest req) {
        return ResponseEntity.status(201).body(service.crear(req));
    }

    @PutMapping("/{id}")
    @RequiresPermission("gestionar_acudientes")
    public Acudiente actualizar(@PathVariable Integer id, @RequestBody AcudienteRequest req) {
        return service.actualizar(id, req);
    }

    @PostMapping("/{id}/acceso")
    @RequiresPermission("gestionar_acudientes")
    public ResponseEntity<AcudienteService.AccesoAcudiente> crearAcceso(@PathVariable Integer id) {
        return ResponseEntity.status(201).body(service.crearAcceso(id));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("gestionar_acudientes")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/deportista/{deportistaId}")
    @RequiresPermission("ver_acudientes")
    public List<VinculoResponse> deDeportista(@PathVariable Integer deportistaId) {
        return service.listarDeDeportista(deportistaId);
    }

    @PostMapping("/deportista/{deportistaId}")
    @RequiresPermission("gestionar_acudientes")
    public ResponseEntity<VinculoResponse> vincular(@PathVariable Integer deportistaId,
            @RequestBody VinculoRequest req) {
        return ResponseEntity.status(201).body(service.vincular(deportistaId, req));
    }

    @DeleteMapping("/vinculos/{vinculoId}")
    @RequiresPermission("gestionar_acudientes")
    public ResponseEntity<Void> desvincular(@PathVariable Integer vinculoId) {
        service.desvincular(vinculoId);
        return ResponseEntity.noContent().build();
    }
}