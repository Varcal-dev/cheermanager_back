package com.varcal.cheermanager.Controller.Legal;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.Models.Legal.SolicitudTitular.Estado;
import com.varcal.cheermanager.Service.Legal.SolicitudTitularService;
import com.varcal.cheermanager.Service.Legal.SolicitudTitularService.Solicitud;
import com.varcal.cheermanager.Service.Legal.SolicitudTitularService.Vista;
import com.varcal.cheermanager.Utils.AuthenticatedUserUtil;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** L8: consultas y reclamos de titulares (Ley 1581). Reusa los permisos de consentimientos. */
@RestController
@RequestMapping("/api/solicitudes-titular")
public class SolicitudTitularController {

    private final SolicitudTitularService service;

    public SolicitudTitularController(SolicitudTitularService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission("ver_consentimientos")
    public List<Vista> listar(@RequestParam(required = false) Estado estado) {
        return service.listar(estado);
    }

    @PostMapping
    @RequiresPermission("gestionar_consentimientos")
    public ResponseEntity<?> registrar(@RequestBody Solicitud body) {
        return ejecutar(201, () -> service.registrar(body, usuario()));
    }

    @PatchMapping("/{id}/en-tramite")
    @RequiresPermission("gestionar_consentimientos")
    public ResponseEntity<?> enTramite(@PathVariable Integer id) {
        return ejecutar(200, () -> service.ponerEnTramite(id));
    }

    /** Body: {"motivo": "..."} */
    @PostMapping("/{id}/prorroga")
    @RequiresPermission("gestionar_consentimientos")
    public ResponseEntity<?> prorrogar(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        return ejecutar(200, () -> service.prorrogar(id, body.get("motivo")));
    }

    /** Body: {"respuesta": "..."} */
    @PostMapping("/{id}/respuesta")
    @RequiresPermission("gestionar_consentimientos")
    public ResponseEntity<?> responder(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        return ejecutar(200, () -> service.responder(id, body.get("respuesta"), usuario()));
    }

    private static int usuario() {
        return AuthenticatedUserUtil.getCurrentUserId().intValue();
    }

    private static ResponseEntity<?> ejecutar(int ok, java.util.function.Supplier<Vista> accion) {
        try {
            return ResponseEntity.status(ok).body(accion.get());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        }
    }
}
