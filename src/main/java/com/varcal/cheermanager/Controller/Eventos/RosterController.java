package com.varcal.cheermanager.Controller.Eventos;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.Models.Eventos.RosterIntegrante.Rol;
import com.varcal.cheermanager.Service.Eventos.RosterService;
import com.varcal.cheermanager.Utils.AuthenticatedUserUtil;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** C2: roster de competencia. Ver = ver_evento; editar = asignar_grupo_evento. */
@RestController
@RequestMapping("/api/rosters")
public class RosterController {

    public record Agregar(Integer deportistaId, Rol rol, String posicion) {
    }

    public record Cambio(Rol rol, String posicion) {
    }

    private final RosterService service;

    public RosterController(RosterService service) {
        this.service = service;
    }

    @GetMapping("/opciones")
    @RequiresPermission("ver_evento")
    public List<RosterService.Opcion> opciones() {
        return service.opciones();
    }

    @GetMapping("/evento/{eventoId}/grupo/{grupoId}")
    @RequiresPermission("ver_evento")
    public ResponseEntity<?> vista(@PathVariable Integer eventoId, @PathVariable Integer grupoId) {
        return ejecutar(() -> service.vista(eventoId, grupoId));
    }

    @PostMapping("/evento/{eventoId}/grupo/{grupoId}/integrantes")
    @RequiresPermission("asignar_grupo_evento")
    public ResponseEntity<?> agregar(@PathVariable Integer eventoId, @PathVariable Integer grupoId, @RequestBody Agregar b) {
        return ejecutar(() -> service.agregar(eventoId, grupoId, b.deportistaId(), b.rol(), b.posicion(), usuario()));
    }

    @PutMapping("/integrantes/{id}")
    @RequiresPermission("asignar_grupo_evento")
    public ResponseEntity<?> cambiar(@PathVariable Integer id, @RequestBody Cambio b) {
        return ejecutar(() -> service.cambiarRol(id, b.rol(), b.posicion()));
    }

    @DeleteMapping("/integrantes/{id}")
    @RequiresPermission("asignar_grupo_evento")
    public ResponseEntity<?> quitar(@PathVariable Integer id) {
        return ejecutar(() -> service.quitar(id));
    }

    /** Body: { "cupoMaximo": 24 } (null = sin tope). */
    @PutMapping("/evento/{eventoId}/grupo/{grupoId}/cupo")
    @RequiresPermission("asignar_grupo_evento")
    public ResponseEntity<?> cupo(@PathVariable Integer eventoId, @PathVariable Integer grupoId, @RequestBody Map<String, Integer> b) {
        return ejecutar(() -> service.cupo(eventoId, grupoId, b.get("cupoMaximo"), usuario()));
    }

    @PostMapping("/evento/{eventoId}/grupo/{grupoId}/confirmar")
    @RequiresPermission("asignar_grupo_evento")
    public ResponseEntity<?> confirmar(@PathVariable Integer eventoId, @PathVariable Integer grupoId) {
        return ejecutar(() -> service.confirmar(eventoId, grupoId));
    }

    @PostMapping("/evento/{eventoId}/grupo/{grupoId}/reabrir")
    @RequiresPermission("asignar_grupo_evento")
    public ResponseEntity<?> reabrir(@PathVariable Integer eventoId, @PathVariable Integer grupoId) {
        return ejecutar(() -> service.reabrir(eventoId, grupoId));
    }

    private ResponseEntity<?> ejecutar(java.util.function.Supplier<?> accion) {
        try {
            return ResponseEntity.ok(accion.get());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private static int usuario() {
        return AuthenticatedUserUtil.getCurrentUserId().intValue();
    }
}
