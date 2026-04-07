package com.varcal.cheermanager.Controller.Persona;

import com.varcal.cheermanager.DTO.Persona.HistorialMedicoDTO;
import com.varcal.cheermanager.Service.Persona.HistorialMedicoService;
import com.varcal.cheermanager.Utils.RequiresPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/historial-medico")
public class HistorialMedicoController {

    @Autowired
    private HistorialMedicoService historialMedicoService;

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
}