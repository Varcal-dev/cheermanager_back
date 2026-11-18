package com.varcal.cheermanager.Controller.clubes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Service.Clubes.ClubPublicoService;

/** Pagina publica "Unete": directorio de clubes y ficha de cada uno (sin sesion; solo lectura). */
@RestController
@RequestMapping("/api/public/clubes")
public class ClubPublicoController {

    @Autowired
    private ClubPublicoService servicio;

    @GetMapping
    public ResponseEntity<?> directorio() {
        return ResponseEntity.ok(servicio.directorio());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<?> ficha(@PathVariable String slug) {
        return servicio.porSlug(slug).<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(java.util.Map.of("message", "Club no encontrado")));
    }
}
