package com.varcal.cheermanager.Controller.Historiales;

import com.varcal.cheermanager.DTO.Persona.HistorialMedicoDTO;
import com.varcal.cheermanager.Service.Persona.HistorialMedicoService;
import com.varcal.cheermanager.Utils.RequiresPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/historial-medico")
public class HistorialMedicoController {

    @Autowired
    private HistorialMedicoService historialMedicoService;

    @PostMapping
    @RequiresPermission("crear_historial_medico")
    public ResponseEntity<?> crear(@RequestBody HistorialMedicoDTO dto) {
        try {
            HistorialMedicoDTO creado = historialMedicoService.crearHistorialMedico(dto);
            return ResponseEntity.status(201).body(creado);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al crear historial médico: " + e.getMessage());
        }
    }

    // Paginado: ?page=0&size=10&sort=fechaRegistro,desc
    @GetMapping("/persona/{personaId}")
    @RequiresPermission("ver_historial_medico")
    public ResponseEntity<?> listarPorPersona(@PathVariable Integer personaId, Pageable pageable) {
        try {
            Page<HistorialMedicoDTO> page = historialMedicoService.obtenerHistorialMedicoPorPersona(personaId, pageable);
            return ResponseEntity.ok(page);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al obtener historial médico: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @RequiresPermission("ver_historial_medico")
    public ResponseEntity<?> obtenerUno(@PathVariable Integer id) {
        return historialMedicoService.obtenerHistorialMedicoPorId(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body("Historial médico no encontrado"));
    }

    @PutMapping("/{id}")
    @RequiresPermission("modificar_historial_medico")
    public ResponseEntity<?> actualizarHistorialMedico(@PathVariable Integer id, @RequestBody HistorialMedicoDTO dto) {
        try {
            HistorialMedicoDTO result = historialMedicoService.actualizarHistorialMedico(id, dto);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al actualizar historial médico: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("eliminar_historial_medico")
    public ResponseEntity<?> eliminarHistorialMedico(@PathVariable Integer id) {
        try {
            historialMedicoService.eliminarHistorialMedico(id);
            return ResponseEntity.ok("Historial médico eliminado exitosamente");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al eliminar historial médico: " + e.getMessage());
        }
    }

    // Alerta rápida para el entrenador antes de una sesión/competencia: ¿este
    // deportista tiene una condición médica con gravedad registrada en los
    // últimos 30 días? (la ventana de 30 días vive en el Service, no aquí).
    @GetMapping("/persona/{personaId}/alerta")
    @RequiresPermission("ver_historial_medico")
    public ResponseEntity<?> alertaCondicionActiva(@PathVariable Integer personaId) {
        boolean tieneCondicionActiva = historialMedicoService.tieneCondicionActiva(personaId);
        return ResponseEntity.ok(Map.of("personaId", personaId, "tieneCondicionActiva", tieneCondicionActiva));
    }
}