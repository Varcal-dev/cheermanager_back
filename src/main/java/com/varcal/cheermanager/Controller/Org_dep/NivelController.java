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

import com.varcal.cheermanager.DTO.Org_dep.NivelDetalleDTO;
import com.varcal.cheermanager.Models.Org_dep.Nivel;
import com.varcal.cheermanager.Service.Org_dep.NivelService;

@RestController
@RequestMapping("/api/niveles")
public class NivelController {

    @Autowired
    private NivelService nivelService;

    // Crear nivel
    @PostMapping
    public ResponseEntity<Nivel> crearNivel(@RequestBody Nivel nivel) {
        Nivel nivelCreado = nivelService.crearNivel(nivel);
        return ResponseEntity.status(HttpStatus.CREATED).body(nivelCreado);
    }

    // Obtener todos los niveles
    @GetMapping
    public List<Nivel> obtenerTodosLosNiveles() {
        return nivelService.obtenerTodosLosNiveles();
    }

    @GetMapping("/detallado")
    public List<NivelDetalleDTO> obtenerNivelesDetallado() {
        return nivelService.obtenerNivelesDetallado();
    }

    // Obtener nivel por ID
    @GetMapping("/{id}")
    public ResponseEntity<Nivel> obtenerNivelPorId(@PathVariable Integer id) {
        try {
            Nivel nivel = nivelService.obtenerNivelPorId(id);
            return ResponseEntity.ok(nivel);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Actualizar nivel
    @PutMapping("/{id}")
    public ResponseEntity<Nivel> actualizarNivel(@PathVariable Integer id, @RequestBody Nivel nivel) {
        try {
            Nivel nivelActualizado = nivelService.actualizarNivel(id, nivel);
            return ResponseEntity.ok(nivelActualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Eliminar nivel
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarNivel(@PathVariable Integer id) {
        try {
            nivelService.eliminarNivel(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
