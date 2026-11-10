package com.varcal.cheermanager.Controller.Horario_Asistencia;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.Service.Horario_Asistencia.SedeService;
import com.varcal.cheermanager.Service.Horario_Asistencia.SedeService.EspacioSolicitud;
import com.varcal.cheermanager.Service.Horario_Asistencia.SedeService.SedeSolicitud;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** C11: sedes, espacios y asignacion de espacio a horarios (ver_sedes / gestionar_sedes). */
@RestController
@RequestMapping("/api/sedes")
public class SedeController {

    private final SedeService service;

    public SedeController(SedeService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission("ver_sedes")
    public List<SedeService.SedeVista> listar() {
        return service.listar();
    }

    @GetMapping("/tablero")
    @RequiresPermission("ver_sedes")
    public SedeService.Tablero tablero() {
        return service.tablero();
    }

    @PostMapping
    @RequiresPermission("gestionar_sedes")
    public ResponseEntity<?> crear(@RequestBody SedeSolicitud body) {
        return ejecutar(201, () -> service.crearSede(body));
    }

    @PutMapping("/{id}")
    @RequiresPermission("gestionar_sedes")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody SedeSolicitud body) {
        return ejecutar(200, () -> service.actualizarSede(id, body));
    }

    @PostMapping("/{sedeId}/espacios")
    @RequiresPermission("gestionar_sedes")
    public ResponseEntity<?> crearEspacio(@PathVariable Integer sedeId, @RequestBody EspacioSolicitud body) {
        return ejecutar(201, () -> service.crearEspacio(sedeId, body));
    }

    @PutMapping("/espacios/{id}")
    @RequiresPermission("gestionar_sedes")
    public ResponseEntity<?> actualizarEspacio(@PathVariable Integer id, @RequestBody EspacioSolicitud body) {
        return ejecutar(200, () -> service.actualizarEspacio(id, body));
    }

    /** Body: { "espacioId": 3 }. Si el horario ya tenia espacio, lo cambia. */
    @PutMapping("/horarios/{horarioEntrenamientoId}/espacio")
    @RequiresPermission("gestionar_sedes")
    public ResponseEntity<?> asignar(@PathVariable Integer horarioEntrenamientoId, @RequestBody Map<String, Integer> body) {
        return ejecutar(200, () -> service.asignar(horarioEntrenamientoId, body.get("espacioId")));
    }

    @DeleteMapping("/horarios/{horarioEntrenamientoId}/espacio")
    @RequiresPermission("gestionar_sedes")
    public ResponseEntity<?> quitar(@PathVariable Integer horarioEntrenamientoId) {
        return ejecutar(200, () -> {
            service.quitarAsignacion(horarioEntrenamientoId);
            return Map.of("ok", true);
        });
    }

    private ResponseEntity<?> ejecutar(int exito, Supplier<?> accion) {
        try {
            return ResponseEntity.status(exito).body(accion.get());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
