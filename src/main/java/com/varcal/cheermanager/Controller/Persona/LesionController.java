package com.varcal.cheermanager.Controller.Persona;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.Service.Persona.LesionService;
import com.varcal.cheermanager.Service.Persona.LesionService.Seguimiento;
import com.varcal.cheermanager.Service.Persona.LesionService.Solicitud;
import com.varcal.cheermanager.Utils.AuthenticatedUserUtil;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** C3: lesiones y retorno al juego (dato de salud: permisos ver_lesiones / gestionar_lesiones). */
@RestController
@RequestMapping("/api/lesiones")
public class LesionController {

    private final LesionService service;

    public LesionController(LesionService service) {
        this.service = service;
    }

    /** ?vigentes=true (por defecto) lista solo las abiertas. */
    @GetMapping
    @RequiresPermission("ver_lesiones")
    public LesionService.Resumen listar(@RequestParam(defaultValue = "true") boolean vigentes) {
        return service.listar(vigentes);
    }

    @GetMapping("/candidatos")
    @RequiresPermission("ver_lesiones")
    public List<LesionService.Candidato> candidatos() {
        return service.candidatos();
    }

    @GetMapping("/{id}")
    @RequiresPermission("ver_lesiones")
    public ResponseEntity<?> detalle(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(service.detalle(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/deportista/{id}")
    @RequiresPermission("ver_lesiones")
    public List<LesionService.Item> historial(@PathVariable Integer id) {
        return service.historial(id);
    }

    @PostMapping("/deportista/{id}")
    @RequiresPermission("gestionar_lesiones")
    public ResponseEntity<?> registrar(@PathVariable Integer id, @RequestBody Solicitud body) {
        try {
            return ResponseEntity.status(201).body(service.registrar(id, body, usuario()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/seguimientos")
    @RequiresPermission("gestionar_lesiones")
    public ResponseEntity<?> seguimiento(@PathVariable Integer id, @RequestBody Seguimiento body) {
        try {
            return ResponseEntity.ok(service.seguimiento(id, body, usuario()));
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
