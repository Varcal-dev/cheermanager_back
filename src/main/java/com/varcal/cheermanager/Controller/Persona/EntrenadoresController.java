package com.varcal.cheermanager.Controller.Persona;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.DTO.Persona.EntrenadorDTO;
import com.varcal.cheermanager.Models.Personas.Entrenador;
import com.varcal.cheermanager.Service.PersonaService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/entrenadores")
public class EntrenadoresController {
    @Autowired
    private PersonaService personaService;

    // Método para registrar un entrenador
    @PostMapping()
    @RequiresPermission("crear_entrenador")
    public ResponseEntity<?> registrarEntrenador(@RequestBody EntrenadorDTO entrenadorDTO) {
        try {
            Entrenador entrenador = personaService.registrarEntrenador(entrenadorDTO);
            return ResponseEntity.ok(entrenador);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al registrar el entrenador: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @RequiresPermission("modificar_entrenador")
    public ResponseEntity<?> modificarEntrenador(@PathVariable Integer id, @RequestBody EntrenadorDTO entrenadorDTO) {
        try {
            Entrenador entrenador = personaService.modificarEntrenador(id, entrenadorDTO);
            return ResponseEntity.ok(entrenador); // Devolver el entrenador actualizado
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al modificar el entrenador: " + e.getMessage());
        }
    }

    @GetMapping()
    @RequiresPermission("ver_entrenador")
    public ResponseEntity<?> listarEntrenadores() {
        try {
            List<Entrenador> entrenadores = personaService.listarEntrenadores();
            return ResponseEntity.ok(entrenadores); // Devolver la lista de entrenadores
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al listar los entrenadores: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("eliminar_entrenador")
    public ResponseEntity<?> eliminarEntrenador(@PathVariable Integer id) {
        try {
            personaService.eliminarEntrenador(id);
            return ResponseEntity.ok("Entrenador eliminado exitosamente");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al eliminar el entrenador: " + e.getMessage());
        }
    }
}
