package com.varcal.cheermanager.Controller.Historiales;

import jakarta.validation.Valid;
import com.varcal.cheermanager.config.ErrorPublico;
import com.varcal.cheermanager.DTO.Persona.HistorialMedicoDTO;
import com.varcal.cheermanager.Service.Historiales.HistorialMedicoService;
import com.varcal.cheermanager.Utils.RequiresPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/historial-medico")
public class HistorialMedicoController {

    @Autowired
    private HistorialMedicoService historialMedicoService;

    // Vista de club completo: todos los registros, con filtros combinables.
    // Alimenta la página /historial-medico (antes 100% mock).
    @GetMapping
    @RequiresPermission("ver_historial_medico")
    public ResponseEntity<Page<HistorialMedicoDTO>> listarTodos(
            @RequestParam(required = false) String tipoRegistro,
            @RequestParam(required = false) String gravedad,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Pageable pageable) {
        return ResponseEntity.ok(
                historialMedicoService.listarTodos(tipoRegistro, gravedad, q, desde, hasta, pageable));
    }

    @PostMapping
    @RequiresPermission("crear_historial_medico")
    public ResponseEntity<?> crear(@Valid @RequestBody HistorialMedicoDTO dto) {
        try {
            HistorialMedicoDTO creado = historialMedicoService.crearHistorialMedico(dto);
            return ResponseEntity.status(201).body(creado);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ErrorPublico.de(e));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al crear historial médico: " + ErrorPublico.de(e));
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
            return ResponseEntity.status(500).body("Error al obtener historial médico: " + ErrorPublico.de(e));
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
    public ResponseEntity<?> actualizarHistorialMedico(@PathVariable Integer id, @Valid @RequestBody HistorialMedicoDTO dto) {
        try {
            HistorialMedicoDTO result = historialMedicoService.actualizarHistorialMedico(id, dto);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ErrorPublico.de(e));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al actualizar historial médico: " + ErrorPublico.de(e));
        }
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("eliminar_historial_medico")
    public ResponseEntity<?> eliminarHistorialMedico(@PathVariable Integer id) {
        try {
            historialMedicoService.eliminarHistorialMedico(id);
            return ResponseEntity.ok("Historial médico eliminado exitosamente");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ErrorPublico.de(e));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al eliminar historial médico: " + ErrorPublico.de(e));
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