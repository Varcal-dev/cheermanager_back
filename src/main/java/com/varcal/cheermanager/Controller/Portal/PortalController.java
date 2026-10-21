package com.varcal.cheermanager.Controller.Portal;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.DTO.Financiero.FacturaResponseDTO;
import com.varcal.cheermanager.DTO.Financiero.PagoResponseDTO;
import com.varcal.cheermanager.Service.Org_dep.SolicitudInscripcionService.SolicitudResponse;
import com.varcal.cheermanager.Service.Portal.PortalService;
import com.varcal.cheermanager.Service.Portal.PortalService.*;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** Portal de deportistas y acudientes. Solo lectura, salvo pedir/cancelar la inscripcion a una temporada. */
@RestController
@RequestMapping("/api/portal")
public class PortalController {

    public record NuevaSolicitud(Integer temporadaId, String observaciones) {
    }

    private final PortalService service;

    public PortalController(PortalService service) {
        this.service = service;
    }

    @GetMapping("/yo")
    @RequiresPermission("acceder_portal")
    public MiPerfil yo() {
        return service.perfil();
    }

    @GetMapping("/deportistas/{id}/facturas")
    @RequiresPermission("acceder_portal")
    public List<FacturaResponseDTO> facturas(@PathVariable Integer id) {
        return service.facturas(id);
    }

    @GetMapping("/deportistas/{id}/pagos")
    @RequiresPermission("acceder_portal")
    public List<PagoResponseDTO> pagos(@PathVariable Integer id) {
        return service.pagos(id);
    }

    @GetMapping("/deportistas/{id}/asistencias")
    @RequiresPermission("acceder_portal")
    public List<AsistenciaItem> asistencias(@PathVariable Integer id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.asistencias(id, desde, hasta);
    }

    @GetMapping("/deportistas/{id}/evaluaciones")
    @RequiresPermission("acceder_portal")
    public List<EvaluacionItem> evaluaciones(@PathVariable Integer id) {
        return service.evaluaciones(id);
    }

    @GetMapping("/deportistas/{id}/objetivos")
    @RequiresPermission("acceder_portal")
    public List<ObjetivoItem> objetivos(@PathVariable Integer id) {
        return service.objetivos(id);
    }

    @GetMapping("/deportistas/{id}/rutinas")
    @RequiresPermission("acceder_portal")
    public List<RutinaItem> rutinas(@PathVariable Integer id) {
        return service.rutinas(id);
    }

    @GetMapping("/temporadas-abiertas")
    @RequiresPermission("acceder_portal")
    public List<TemporadaItem> temporadasAbiertas() {
        return service.temporadasAbiertas();
    }

    @GetMapping("/deportistas/{id}/solicitudes")
    @RequiresPermission("acceder_portal")
    public List<SolicitudResponse> solicitudes(@PathVariable Integer id) {
        return service.solicitudes(id);
    }

    @PostMapping("/deportistas/{id}/solicitudes")
    @RequiresPermission("acceder_portal")
    public ResponseEntity<SolicitudResponse> solicitar(@PathVariable Integer id, @RequestBody NuevaSolicitud req) {
        return ResponseEntity.status(201).body(service.solicitar(id, req.temporadaId(), req.observaciones()));
    }

    @PatchMapping("/solicitudes/{id}/cancelar")
    @RequiresPermission("acceder_portal")
    public SolicitudResponse cancelar(@PathVariable Integer id) {
        return service.cancelarSolicitud(id);
    }
}
