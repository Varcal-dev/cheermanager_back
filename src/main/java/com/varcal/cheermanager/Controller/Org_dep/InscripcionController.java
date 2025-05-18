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

import com.varcal.cheermanager.DTO.Org_dep.InscripcionDto;
import com.varcal.cheermanager.DTO.Org_dep.InscripcionSimpleDTO;
import com.varcal.cheermanager.Models.Org_dep.Inscripcion;
import com.varcal.cheermanager.Service.Org_dep.InscripcionService;

@RestController
@RequestMapping("/api/inscripciones")
public class InscripcionController {

    @Autowired
    private InscripcionService inscripcionService;

    @GetMapping("/detalle")
    public ResponseEntity<List<Object[]>> getDetalleInscripciones() {
        return ResponseEntity.ok(inscripcionService.obtenerDetalleInscripciones());
    }

    @GetMapping("/{id}")
    public Inscripcion obtenerUna(@PathVariable Integer id) {
        return inscripcionService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<Inscripcion> crear(@RequestBody InscripcionDto inscripcionDto) {
        Inscripcion nueva = inscripcionService.crearInscripcion(inscripcionDto);
        return new ResponseEntity<>(nueva, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Inscripcion> actualizar(@PathVariable Integer id, @RequestBody Inscripcion inscripcion) {
        Inscripcion actualizada = inscripcionService.actualizarInscripcion(id, inscripcion);
        return ResponseEntity.ok(actualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        inscripcionService.eliminarInscripcion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/simple")
    public ResponseEntity<List<InscripcionSimpleDTO>> obtenerInscripciones() {
        List<InscripcionSimpleDTO> inscripciones = inscripcionService.listarInscripcionesSimples();
        return ResponseEntity.ok(inscripciones);
    }

    @GetMapping("/no-inscritos")
    public ResponseEntity<List<Object[]>> getDeportistasNoInscritos() {
        return ResponseEntity.ok(inscripcionService.obtenerDeportistasNoInscritos());
    }

}
