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
import java.util.List;

@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    @Autowired
    private PersonaService personaService;

    @PostMapping("/registrar")
    public ResponseEntity<?> registrarPersona(@RequestBody PersonaDTO personaDTO) {
        try {
            Persona persona = personaService.registrarPersona(personaDTO);
            return ResponseEntity.ok(persona); // Devolver la persona registrada
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al registrar la persona: " + e.getMessage());
        }
    }

    @PutMapping("/modificar/{id}")
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

    // Método para registrar un deportista
    @PostMapping("/registrar/deportista")
    public ResponseEntity<?> registrarDeportista(@RequestBody DeportistaDTO deportistaDTO) {
        try {
            Deportista deportista = personaService.registrarDeportista(deportistaDTO);
            return ResponseEntity.ok(deportista);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al registrar el deportista: " + e.getMessage());
        }
    }

    @PutMapping("/modificar/deportista/{id}")
    public ResponseEntity<?> modificarDeportista(@PathVariable Integer id, @RequestBody DeportistaDTO deportistaDTO) {
        try {
            Deportista deportista = personaService.modificarDeportista(id, deportistaDTO);
            return ResponseEntity.ok(deportista); // Devolver el deportista actualizado
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al modificar el deportista: " + e.getMessage());
        }
    }

    @GetMapping("/listar/deportistas")
    public ResponseEntity<?> listarDeportistasConDetalles() {
        try {
            List<DeportistaVistaDTO> deportistas = personaService.listarDeportistasConDetalles();
            return ResponseEntity.ok(deportistas); // Devolver la lista de deportistas con detalles
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al listar los deportistas: " + e.getMessage());
        }
    }

    @DeleteMapping("/eliminar/deportista/{id}")
    public ResponseEntity<?> eliminarDeportista(@PathVariable Integer id) {
        try {
            personaService.eliminarDeportista(id);
            return ResponseEntity.ok("Deportista eliminado exitosamente");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al eliminar el deportista: " + e.getMessage());
        }
    }

    // Método para registrar un entrenador
    @PostMapping("/registrar/entrenador")
    public ResponseEntity<?> registrarEntrenador(@RequestBody EntrenadorDTO entrenadorDTO) {
        try {
            Entrenador entrenador = personaService.registrarEntrenador(entrenadorDTO);
            return ResponseEntity.ok(entrenador);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al registrar el entrenador: " + e.getMessage());
        }
    }

    @PutMapping("/modificar/entrenador/{id}")
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
    public ResponseEntity<?> listarEntrenadores() {
        try {
            List<Entrenador> entrenadores = personaService.listarEntrenadores();
            return ResponseEntity.ok(entrenadores); // Devolver la lista de entrenadores
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al listar los entrenadores: " + e.getMessage());
        }
    }

    @DeleteMapping("/eliminar/entrenador/{id}")
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