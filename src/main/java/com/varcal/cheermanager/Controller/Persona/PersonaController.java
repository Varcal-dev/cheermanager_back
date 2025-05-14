package com.varcal.cheermanager.Controller.Persona;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.DTO.Persona.DeportistaDTO;
import com.varcal.cheermanager.DTO.Persona.DeportistaVistaDTO;
import com.varcal.cheermanager.DTO.Persona.EntrenadorDTO;
import com.varcal.cheermanager.DTO.Persona.PersonaDTO;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.Entrenador;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.PersonaService;
import com.varcal.cheermanager.Utils.RequiresPermission;

import java.util.List;

@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    @Autowired
    private PersonaService personaService;

    @PostMapping("/registrar")
    @RequiresPermission("crear_persona")
    public ResponseEntity<?> registrarPersona(@RequestBody PersonaDTO personaDTO) {
        try {
            Persona persona = personaService.registrarPersona(personaDTO);
            return ResponseEntity.ok(persona); // Devolver la persona registrada
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al registrar la persona: " + e.getMessage());
        }
    }

    @PutMapping("/modificar/{id}")
    @RequiresPermission("modificar_persona")
    public ResponseEntity<?> modificarPersona(@PathVariable Integer id, @RequestBody PersonaDTO personaDTO) {
        try {
            Persona persona = personaService.modificarPersona(id, personaDTO);
            return ResponseEntity.ok(persona); // Devolver la persona actualizada
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al modificar la persona: " + e.getMessage());
        }
    }

    @GetMapping("/listar")
    @RequiresPermission("ver_persona")
    public ResponseEntity<?> listarPersonas() {
        try {
            List<Persona> personas = personaService.listarPersonas();
            return ResponseEntity.ok(personas); // Devolver la lista de personas
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al listar las personas: " + e.getMessage());
        }
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Persona>> buscarPersonasByNombre(@RequestParam String nombre) {
        List<Persona> personas = personaService.buscarPersonasByNombre(nombre);
        return ResponseEntity.ok(personas);
    }

    @DeleteMapping("/eliminar/{id}")
    @RequiresPermission("eliminar_persona")
    public ResponseEntity<?> eliminarPersona(@PathVariable Integer id) {
        try {
            personaService.eliminarPersona(id);
            return ResponseEntity.ok("Persona eliminada exitosamente");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al eliminar la persona: " + e.getMessage());
        }
    }

   

    // Método para registrar un entrenador
    @PostMapping("/registrar/entrenador")
    @RequiresPermission("crear_entrenador")
    public ResponseEntity<?> registrarEntrenador(@RequestBody EntrenadorDTO entrenadorDTO) {
        try {
            Entrenador entrenador = personaService.registrarEntrenador(entrenadorDTO);
            return ResponseEntity.ok(entrenador);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al registrar el entrenador: " + e.getMessage());
        }
    }

    @PutMapping("/modificar/entrenador/{id}")
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

    @GetMapping("/listar/entrenadores")
    @RequiresPermission("ver_entrenador")
    public ResponseEntity<?> listarEntrenadores() {
        try {
            List<Entrenador> entrenadores = personaService.listarEntrenadores();
            return ResponseEntity.ok(entrenadores); // Devolver la lista de entrenadores
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al listar los entrenadores: " + e.getMessage());
        }
    }

    @DeleteMapping("/eliminar/entrenador/{id}")
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