package com.varcal.cheermanager.Controller.Persona;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Service.Persona.CopiaDatosService;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** Copia de los datos de un titular (derecho de acceso, L6). Incluye datos de salud: permiso propio. */
@RestController
@RequestMapping("/api/personas")
public class CopiaDatosController {

    private final CopiaDatosService service;

    public CopiaDatosController(CopiaDatosService service) {
        this.service = service;
    }

    @GetMapping("/{personaId}/copia-datos")
    @RequiresPermission("exportar_datos_titular")
    public ResponseEntity<?> copia(@PathVariable Integer personaId) {
        try {
            return ResponseEntity.ok(service.copia(personaId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }
}
