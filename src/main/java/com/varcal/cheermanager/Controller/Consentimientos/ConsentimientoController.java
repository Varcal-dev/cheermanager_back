package com.varcal.cheermanager.Controller.Consentimientos;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.config.ErrorPublico;
import com.varcal.cheermanager.Models.Consentimientos.Consentimiento.Tipo;
import com.varcal.cheermanager.Service.Consentimientos.ConsentimientoService;
import com.varcal.cheermanager.Service.Consentimientos.ConsentimientoService.OtorgarRequest;
import com.varcal.cheermanager.Service.Consentimientos.ConsentimientoService.Pendiente;
import com.varcal.cheermanager.Service.Consentimientos.ConsentimientoService.Resumen;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/consentimientos")
public class ConsentimientoController {

    private final ConsentimientoService service;

    public ConsentimientoController(ConsentimientoService service) {
        this.service = service;
    }

    @PostMapping
    @RequiresPermission("gestionar_consentimientos")
    public ResponseEntity<Resumen> otorgar(@jakarta.validation.Valid @RequestBody OtorgarRequest req) {
        return ResponseEntity.status(201).body(service.otorgar(req));
    }

    @PatchMapping("/{id}/revocar")
    @RequiresPermission("gestionar_consentimientos")
    public Resumen revocar(@PathVariable Integer id, @RequestBody(required = false) Map<String, String> body) {
        return service.revocar(id, body == null ? null : body.get("motivo"));
    }

    @GetMapping("/persona/{personaId}")
    @RequiresPermission("ver_consentimientos")
    public List<Resumen> deLaPersona(@PathVariable Integer personaId) {
        return service.listarDePersona(personaId);
    }

    /** Deportistas sin consentimiento vigente del tipo indicado (por defecto, datos de salud). */
    @GetMapping("/pendientes")
    @RequiresPermission("ver_consentimientos")
    public List<Pendiente> pendientes(@RequestParam(defaultValue = "DATOS_SALUD") Tipo tipo) {
        return service.pendientes(tipo);
    }

    /** Antes un cuerpo vacio ({}) equivalia a activa=false y desactivaba la exigencia sin avisar. */
    public record ExigenciaRequest(@jakarta.validation.constraints.NotNull(message = "Indica si la exigencia esta activa (true o false)") Boolean activa) {
    }

    @GetMapping("/exigencia-salud")
    @RequiresPermission("ver_consentimientos")
    public ConsentimientoService.Exigencia exigencia() {
        return service.exigencia();
    }

    /** Body: {"activa": true|false}. Activar se rechaza (400) mientras haya pendientes. */
    @PutMapping("/exigencia-salud")
    @RequiresPermission("gestionar_consentimientos")
    public ResponseEntity<?> configurarExigencia(@jakarta.validation.Valid @RequestBody ExigenciaRequest body) {
        try {
            return ResponseEntity.ok(service.configurarExigencia(body.activa()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", ErrorPublico.de(e)));
        }
    }
}
