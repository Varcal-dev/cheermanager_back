package com.varcal.cheermanager.Controller.Clubes;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Service.Clubes.ExportacionFuncionService;
import com.varcal.cheermanager.Service.Clubes.FuncionesService;
import com.varcal.cheermanager.Utils.RequiresPermission;
import com.varcal.cheermanager.Utils.SinPermisoEspecifico;
import com.varcal.cheermanager.tenant.TenantContext;

/**
 * Funciones del plan del club (V39) y sus ventanas al bajar de plan (V41).
 *  - GET /api/suscripcion/funciones: que trae el plan y que funciones esta perdiendo (el menu y "Plan y mis datos").
 *  - GET /api/suscripcion/funciones/{codigo}/exportar: ZIP con los datos de esa funcion, siempre disponible.
 */
@RestController
@RequestMapping("/api/suscripcion")
public class FuncionesController {

    private final FuncionesService funciones;
    private final ExportacionFuncionService exportacion;

    public FuncionesController(FuncionesService funciones, ExportacionFuncionService exportacion) {
        this.funciones = funciones;
        this.exportacion = exportacion;
    }

    @GetMapping("/funciones")
    @SinPermisoEspecifico("lo consulta el menu de cualquier usuario del club; solo dice que funciones trae el plan")
    public Map<String, Object> propias() {
        int clubId = TenantContext.require();
        FuncionesService.Habilitadas h = funciones.habilitadas(clubId);
        List<FuncionesService.Perdida> perdidas = funciones.perdidasDelClub(clubId);
        return Map.of("todas", h.todas(), "funciones", new TreeSet<>(h.codigos()), "perdidas", perdidas);
    }

    @GetMapping("/funciones/{codigo}/exportar")
    @RequiresPermission("exportar_listados")
    public ResponseEntity<byte[]> exportar(@PathVariable String codigo) throws IOException {
        byte[] zip = exportacion.zip(TenantContext.require(), codigo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"cheermanager-" + codigo.toLowerCase() + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zip);
    }
}
