package com.varcal.cheermanager.Controller.Clubes;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Service.Clubes.PaquetesService;
import com.varcal.cheermanager.Utils.RequiresPermission;
import com.varcal.cheermanager.Utils.SinPermisoEspecifico;
import com.varcal.cheermanager.tenant.TenantContext;

/**
 * Paquetes de deportistas adicionales.
 *  - GET /api/suscripcion/paquetes: el club ve los paquetes de su plan y los que tiene.
 *  - /api/clubes/{id}/paquetes: el superadmin (gestionar_clubes) los asigna y los termina.
 */
@RestController
@RequestMapping("/api")
public class PaquetesController {

    private final PaquetesService paquetes;

    public PaquetesController(PaquetesService paquetes) {
        this.paquetes = paquetes;
    }

    @GetMapping("/suscripcion/paquetes")
    @SinPermisoEspecifico("lo ve cualquier usuario del club junto al aviso de cupo; solo muestra el catalogo de su plan y sus paquetes")
    public PaquetesService.Vista propios() {
        return paquetes.vista(TenantContext.require());
    }

    @GetMapping("/clubes/{id}/paquetes")
    @RequiresPermission("gestionar_clubes")
    public PaquetesService.Vista de(@PathVariable Integer id) {
        return paquetes.vista(id);
    }

    @PostMapping("/clubes/{id}/paquetes")
    @RequiresPermission("gestionar_clubes")
    public ResponseEntity<PaquetesService.Vista> asignar(@PathVariable Integer id,
            @RequestBody PaquetesService.AsignarRequest request) {
        String quien = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;
        return ResponseEntity.status(201).body(paquetes.asignar(id, request, quien));
    }

    @DeleteMapping("/clubes/{id}/paquetes/{asignacionId}")
    @RequiresPermission("gestionar_clubes")
    public PaquetesService.Vista terminar(@PathVariable Integer id, @PathVariable Integer asignacionId) {
        return paquetes.terminar(id, asignacionId);
    }
}
