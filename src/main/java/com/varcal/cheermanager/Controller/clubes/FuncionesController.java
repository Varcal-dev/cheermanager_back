package com.varcal.cheermanager.Controller.Clubes;

import java.util.Map;
import java.util.TreeSet;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Service.Clubes.FuncionesService;
import com.varcal.cheermanager.Utils.SinPermisoEspecifico;
import com.varcal.cheermanager.tenant.TenantContext;

/** GET /api/suscripcion/funciones: que funciones trae el plan del club (el front oculta del menu las que no). */
@RestController
@RequestMapping("/api/suscripcion")
public class FuncionesController {

    private final FuncionesService funciones;

    public FuncionesController(FuncionesService funciones) {
        this.funciones = funciones;
    }

    @GetMapping("/funciones")
    @SinPermisoEspecifico("lo consulta el menu de cualquier usuario del club; solo dice que funciones trae el plan")
    public Map<String, Object> propias() {
        FuncionesService.Habilitadas h = funciones.habilitadas(TenantContext.require());
        return Map.of("todas", h.todas(), "funciones", new TreeSet<>(h.codigos()));
    }
}
