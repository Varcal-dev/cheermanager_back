package com.varcal.cheermanager.Controller.Persona;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.DTO.Persona.PersonaDTO;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.PersonaService;
import com.varcal.cheermanager.Utils.RequiresPermission;

import java.util.List;

@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    @Autowired
    private PersonaService personaService;

    @PostMapping()
    @RequiresPermission("crear_persona")
    public ResponseEntity<?> registrarPersona(@RequestBody PersonaDTO personaDTO) {
        try {
            Persona persona = personaService.registrarPersona(personaDTO);
            return ResponseEntity.ok(persona); // Devolver la persona registrada
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al registrar la persona: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
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

    @GetMapping()
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

    @DeleteMapping("/{id}")
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

}