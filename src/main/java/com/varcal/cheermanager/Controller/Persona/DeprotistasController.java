package com.varcal.cheermanager.Controller.Persona;

import java.util.List;
import java.util.Optional;

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

import com.varcal.cheermanager.DTO.Persona.DeportistaDTO;
import com.varcal.cheermanager.DTO.Persona.DeportistaVistaDTO;
import com.varcal.cheermanager.Models.Financiero.Convenio;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Service.Persona.PersonaService;
import com.varcal.cheermanager.Utils.RequiresPermission;
import com.varcal.cheermanager.repository.Financiero.ConvenioRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

@RestController
@RequestMapping("/api/deportistas")
public class DeprotistasController {
    @Autowired
    private PersonaService personaService;

    @Autowired
    private DeportistaRepository deportistaRepository;

    @Autowired
    private ConvenioRepository convenioRepository;

    // Método para registrar un deportista
    @PostMapping()
    @RequiresPermission("crear_deportista")
    public ResponseEntity<?> registrarDeportista(@RequestBody DeportistaDTO deportistaDTO) {
        try {
            Deportista deportista = personaService.registrarDeportista(deportistaDTO);
            return ResponseEntity.ok(deportista);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al registrar el deportista: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @RequiresPermission("modificar_deportista")
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

    @GetMapping()
    @RequiresPermission("ver_deportista")
    public ResponseEntity<?> listarDeportistasConDetalles() {
        try {
            List<DeportistaVistaDTO> deportistas = personaService.listarDeportistasConDetalles();
            return ResponseEntity.ok(deportistas); // Devolver la lista de deportistas con detalles
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al listar los deportistas: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("eliminar_deportista")
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

    @PutMapping("/{id}/convenio")
    public ResponseEntity<?> asignarConvenio(
            @PathVariable Integer id,
            @RequestBody Integer convenioId) {
        Optional<Deportista> optional = deportistaRepository.findById(id);
        Optional<Convenio> convenio = convenioRepository.findById(convenioId);

        if (optional.isEmpty() || convenio.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Deportista deportista = optional.get();
        deportista.setConvenioId(convenio.get().getId());
        deportistaRepository.save(deportista);

        return ResponseEntity.ok("Convenio asignado correctamente.");
    }

}
