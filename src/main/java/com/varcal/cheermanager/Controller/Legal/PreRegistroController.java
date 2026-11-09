package com.varcal.cheermanager.Controller.Legal;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.Models.Legal.PreRegistro;
import com.varcal.cheermanager.Service.Legal.PreRegistroService;
import com.varcal.cheermanager.Utils.RequiresPermission;
import com.varcal.cheermanager.Utils.SinPermisoEspecifico;

import jakarta.servlet.http.HttpServletRequest;

/** Pre-registro de la landing (POST publico) y su gestion por el superadmin (gestionar_clubes). */
@RestController
public class PreRegistroController {

    private final PreRegistroService service;

    public PreRegistroController(PreRegistroService service) {
        this.service = service;
    }

    @PostMapping("/api/public/preregistro")
    @SinPermisoEspecifico("endpoint publico de la landing (abierto en SecurityConfig); valida, limita por IP y registra el consentimiento")
    public ResponseEntity<?> registrar(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            service.registrar(body, LegalController.ipDe(request));
            // Misma respuesta si se guardo o ya estaba: no revela si un correo ya se pre-registro.
            return ResponseEntity.status(201).body(Map.of("ok", true));
        } catch (PreRegistroService.TooManyRequests e) {
            return ResponseEntity.status(429).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/api/plataforma/preregistros")
    @RequiresPermission("gestionar_clubes")
    public List<PreRegistro> listar() {
        return service.listar();
    }

    /** Body: { "estado": "CONTACTADO", "notas": "..." }. */
    @PutMapping("/api/plataforma/preregistros/{id}")
    @RequiresPermission("gestionar_clubes")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(service.actualizar(id, body.get("estado"), body.get("notas")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
