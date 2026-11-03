package com.varcal.cheermanager.Controller.Clubes;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Models.Clubes.SuscripcionPago;
import com.varcal.cheermanager.Service.Clubes.SuscripcionService;
import com.varcal.cheermanager.Utils.RequiresPermission;
import com.varcal.cheermanager.Utils.SinPermisoEspecifico;

/**
 * Suscripcion de los clubes.
 *  - GET /api/suscripcion: el club del usuario ve su plan, vigencia y uso (cualquier usuario autenticado; lo usa el aviso del front).
 *  - /api/clubes/...: administracion de planes y suscripciones, solo superadmin (gestionar_clubes).
 */
@RestController
@RequestMapping("/api")
public class SuscripcionController {

    private final SuscripcionService suscripcionService;

    public SuscripcionController(SuscripcionService suscripcionService) {
        this.suscripcionService = suscripcionService;
    }

    @GetMapping("/suscripcion")
    @SinPermisoEspecifico("el aviso de suscripcion lo ve cualquier usuario del club; no incluye notas internas")
    public SuscripcionService.Resumen propia() {
        return suscripcionService.resumenDelClubActual();
    }

    @GetMapping("/clubes/planes")
    @RequiresPermission("gestionar_clubes")
    public List<SuscripcionService.PlanDTO> planes() {
        return suscripcionService.planes();
    }

    @GetMapping("/clubes/suscripciones")
    @RequiresPermission("gestionar_clubes")
    public List<SuscripcionService.Resumen> todas() {
        return suscripcionService.resumenes();
    }

    @GetMapping("/clubes/{id}/suscripcion")
    @RequiresPermission("gestionar_clubes")
    public SuscripcionService.Resumen obtener(@PathVariable Integer id) {
        return suscripcionService.resumen(id);
    }

    @PutMapping("/clubes/{id}/suscripcion")
    @RequiresPermission("gestionar_clubes")
    public SuscripcionService.Resumen actualizar(@PathVariable Integer id,
            @RequestBody SuscripcionService.ActualizarRequest request) {
        return suscripcionService.actualizar(id, request);
    }

    @GetMapping("/clubes/{id}/suscripcion/pagos")
    @RequiresPermission("gestionar_clubes")
    public List<SuscripcionPago> pagos(@PathVariable Integer id) {
        return suscripcionService.pagos(id);
    }

    /** Registra un cobro y extiende la vigencia (ver SuscripcionService.registrarPago). */
    @PostMapping("/clubes/{id}/suscripcion/pagos")
    @RequiresPermission("gestionar_clubes")
    public ResponseEntity<SuscripcionService.Resumen> registrarPago(@PathVariable Integer id,
            @RequestBody SuscripcionService.PagoRequest request) {
        String quien = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;
        return ResponseEntity.status(201).body(suscripcionService.registrarPago(id, request, quien));
    }
}
