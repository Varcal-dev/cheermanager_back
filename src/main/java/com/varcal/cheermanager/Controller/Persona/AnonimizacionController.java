package com.varcal.cheermanager.Controller.Persona;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Service.Persona.AnonimizacionService;
import com.varcal.cheermanager.Utils.AuthenticatedUserUtil;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** Anonimizacion de un titular (supresion de datos personales, L7). IRREVERSIBLE. */
@RestController
@RequestMapping("/api/personas")
public class AnonimizacionController {

    private final AnonimizacionService service;

    public AnonimizacionController(AnonimizacionService service) {
        this.service = service;
    }

    /** Body: { "motivo": "Solicitud del titular #12", "confirmacion": "ANONIMIZAR" }. */
    @PostMapping("/{personaId}/anonimizar")
    @RequiresPermission("anonimizar_titular")
    public ResponseEntity<?> anonimizar(@PathVariable Integer personaId, @RequestBody AnonimizacionService.Solicitud body) {
        try {
            return ResponseEntity.ok(service.anonimizar(personaId, body,
                    AuthenticatedUserUtil.getCurrentUserId().intValue()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        }
    }
}
