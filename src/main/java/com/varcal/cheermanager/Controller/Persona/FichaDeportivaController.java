package com.varcal.cheermanager.Controller.Persona;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.Service.Persona.FichaDeportivaService;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** Ficha deportiva: experiencia declarada (nivel y habilidades) y tallas de uniforme. */
@RestController
@RequestMapping("/api/deportistas")
public class FichaDeportivaController {

    private final FichaDeportivaService servicio;

    public FichaDeportivaController(FichaDeportivaService servicio) {
        this.servicio = servicio;
    }

    /** Glosario N1-N4 para el formulario de registro (no exige permisos de evaluacion). */
    @GetMapping("/ficha/habilidades")
    @RequiresPermission("crear_deportista")
    public ResponseEntity<?> catalogo() {
        return ResponseEntity.ok(servicio.catalogo());
    }

    @GetMapping("/{id}/ficha")
    @RequiresPermission("ver_deportista")
    public ResponseEntity<?> obtener(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(servicio.obtener(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/ficha")
    @RequiresPermission("modificar_deportista")
    public ResponseEntity<?> guardar(@PathVariable Integer id, @RequestBody FichaDeportivaService.Solicitud solicitud) {
        try {
            return ResponseEntity.ok(servicio.guardar(id, solicitud));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
