package com.varcal.cheermanager.Controller.Persona;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.Service.Persona.CertificacionEntrenadorService;
import com.varcal.cheermanager.Service.Persona.HorasEntrenadorService;
import com.varcal.cheermanager.Utils.AuthenticatedUserUtil;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** C10: certificaciones y horas dictadas de los entrenadores. */
@RestController
@RequestMapping("/api/entrenador-gestion")
public class EntrenadorGestionController {

    private final CertificacionEntrenadorService certificaciones;
    private final HorasEntrenadorService horas;

    public EntrenadorGestionController(CertificacionEntrenadorService certificaciones, HorasEntrenadorService horas) {
        this.certificaciones = certificaciones;
        this.horas = horas;
    }

    @GetMapping("/certificaciones/alertas")
    @RequiresPermission("ver_entrenador")
    public ResponseEntity<?> alertas(@RequestParam(defaultValue = "30") int dias,
            @RequestParam(defaultValue = "false") boolean todos) {
        return ejecutar(200, () -> certificaciones.alertas(dias, todos));
    }

    @GetMapping("/certificaciones/{entrenadorId}")
    @RequiresPermission("ver_entrenador")
    public List<CertificacionEntrenadorService.Certificacion> historial(@PathVariable Integer entrenadorId) {
        return certificaciones.historial(entrenadorId);
    }

    @PostMapping("/certificaciones/{entrenadorId}")
    @RequiresPermission("modificar_entrenador")
    public ResponseEntity<?> registrarCertificacion(@PathVariable Integer entrenadorId,
            @RequestBody CertificacionEntrenadorService.Solicitud body) {
        return ejecutar(201, () -> certificaciones.registrar(entrenadorId, body, usuario()));
    }

    @GetMapping("/horas")
    @RequiresPermission("ver_entrenador")
    public ResponseEntity<?> periodo(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ejecutar(200, () -> horas.periodo(desde, hasta));
    }

    @PostMapping("/horas")
    @RequiresPermission("modificar_entrenador")
    public ResponseEntity<?> registrarHoras(@RequestBody HorasEntrenadorService.Solicitud body) {
        return ejecutar(201, () -> horas.registrar(body, usuario()));
    }

    @DeleteMapping("/horas/{id}")
    @RequiresPermission("modificar_entrenador")
    public ResponseEntity<?> eliminarHoras(@PathVariable Integer id) {
        return ejecutar(200, () -> {
            horas.eliminar(id);
            return Map.of("ok", true);
        });
    }

    @PostMapping("/horas/precargar")
    @RequiresPermission("modificar_entrenador")
    public ResponseEntity<?> precargar(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ejecutar(200, () -> horas.precargar(desde, hasta, usuario()));
    }

    private static int usuario() {
        return AuthenticatedUserUtil.getCurrentUserId().intValue();
    }

    private static ResponseEntity<?> ejecutar(int ok, Supplier<Object> accion) {
        try {
            return ResponseEntity.status(ok).body(accion.get());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
