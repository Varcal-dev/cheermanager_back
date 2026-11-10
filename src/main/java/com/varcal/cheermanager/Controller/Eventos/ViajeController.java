package com.varcal.cheermanager.Controller.Eventos;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.Models.Eventos.ViajeParticipante;
import com.varcal.cheermanager.Service.Eventos.ViajeService;
import com.varcal.cheermanager.Utils.AuthenticatedUserUtil;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** C5: viaje a competencia (ver_viajes / gestionar_viajes). */
@RestController
@RequestMapping("/api/viajes")
public class ViajeController {

    public record Acompanante(String nombre, String documento, ViajeParticipante.RolAcompanante rol) {
    }

    public record CambioParticipante(ViajeParticipante.Estado estado, Boolean autorizacion, String notas) {
    }

    public record Abono(BigDecimal monto, String nota) {
    }

    private final ViajeService service;

    public ViajeController(ViajeService service) {
        this.service = service;
    }

    @GetMapping("/eventos")
    @RequiresPermission("ver_viajes")
    public List<ViajeService.Opcion> opciones() {
        return service.opciones();
    }

    @GetMapping("/{id}")
    @RequiresPermission("ver_viajes")
    public ResponseEntity<?> vista(@PathVariable Integer id) {
        return ejecutar(200, () -> service.vista(id));
    }

    @PostMapping("/evento/{eventoId}")
    @RequiresPermission("gestionar_viajes")
    public ResponseEntity<?> crear(@PathVariable Integer eventoId, @RequestBody ViajeService.Solicitud body) {
        return ejecutar(201, () -> service.crear(eventoId, body, usuario()));
    }

    @PutMapping("/{id}")
    @RequiresPermission("gestionar_viajes")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody ViajeService.Solicitud body) {
        return ejecutar(200, () -> service.actualizar(id, body));
    }

    /** Agrega a la delegacion los deportistas de los rosters del evento. */
    @PostMapping("/{id}/delegacion")
    @RequiresPermission("gestionar_viajes")
    public ResponseEntity<?> delegacion(@PathVariable Integer id) {
        return ejecutar(200, () -> {
            int n = service.cargarDelegacion(id);
            return Map.of("agregados", n, "viaje", service.vista(id));
        });
    }

    @PostMapping("/{id}/acompanantes")
    @RequiresPermission("gestionar_viajes")
    public ResponseEntity<?> acompanante(@PathVariable Integer id, @RequestBody Acompanante b) {
        return ejecutar(201, () -> service.agregarAcompanante(id, b.nombre(), b.documento(), b.rol()));
    }

    @PutMapping("/participantes/{id}")
    @RequiresPermission("gestionar_viajes")
    public ResponseEntity<?> participante(@PathVariable Integer id, @RequestBody CambioParticipante b) {
        return ejecutar(200, () -> service.actualizarParticipante(id, b.estado(), b.autorizacion(), b.notas()));
    }

    @DeleteMapping("/participantes/{id}")
    @RequiresPermission("gestionar_viajes")
    public ResponseEntity<?> quitar(@PathVariable Integer id) {
        return ejecutar(200, () -> service.quitarParticipante(id));
    }

    @PostMapping("/participantes/{id}/abonos")
    @RequiresPermission("gestionar_viajes")
    public ResponseEntity<?> abonar(@PathVariable Integer id, @RequestBody Abono b) {
        return ejecutar(201, () -> service.abonar(id, b.monto(), b.nota(), usuario()));
    }

    @PostMapping("/{id}/cerrar")
    @RequiresPermission("gestionar_viajes")
    public ResponseEntity<?> cerrar(@PathVariable Integer id) {
        return ejecutar(200, () -> service.cerrar(id));
    }

    @PostMapping("/{id}/reabrir")
    @RequiresPermission("gestionar_viajes")
    public ResponseEntity<?> reabrir(@PathVariable Integer id) {
        return ejecutar(200, () -> service.reabrir(id));
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

    private static int usuario() {
        return AuthenticatedUserUtil.getCurrentUserId().intValue();
    }
}
