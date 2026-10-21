package com.varcal.cheermanager.Controller.Org_dep;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.Service.Org_dep.SolicitudInscripcionService;
import com.varcal.cheermanager.Service.Org_dep.SolicitudInscripcionService.AprobarRequest;
import com.varcal.cheermanager.Service.Org_dep.SolicitudInscripcionService.SolicitudResponse;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** Lado del CLUB: revisar, aprobar o rechazar las solicitudes de inscripcion a temporada. */
@RestController
@RequestMapping("/api/solicitudes-inscripcion")
public class SolicitudInscripcionController {

    public record RechazoRequest(String motivo) {
    }

    private final SolicitudInscripcionService service;

    public SolicitudInscripcionController(SolicitudInscripcionService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission("gestionar_inscripciones")
    public List<SolicitudResponse> listar(@RequestParam(required = false) String estado) {
        return service.listar(estado);
    }

    @PatchMapping("/{id}/aprobar")
    @RequiresPermission("gestionar_inscripciones")
    public SolicitudResponse aprobar(@PathVariable Integer id, @RequestBody AprobarRequest req) {
        return service.aprobar(id, req);
    }

    @PatchMapping("/{id}/rechazar")
    @RequiresPermission("gestionar_inscripciones")
    public SolicitudResponse rechazar(@PathVariable Integer id, @RequestBody RechazoRequest req) {
        return service.rechazar(id, req == null ? null : req.motivo());
    }
}
