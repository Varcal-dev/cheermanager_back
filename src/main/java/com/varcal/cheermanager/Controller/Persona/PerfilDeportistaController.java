package com.varcal.cheermanager.Controller.Persona;

import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Service.Auth.UsuarioService;
import com.varcal.cheermanager.Service.Perfil.PerfilDeportistaService;
import com.varcal.cheermanager.Utils.AuthenticatedUserUtil;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** Perfil 360 del deportista para el personal del club (solo lectura). */
@RestController
@RequestMapping("/api/deportistas")
public class PerfilDeportistaController {

    private final PerfilDeportistaService service;
    private final UsuarioService usuarioService;

    public PerfilDeportistaController(PerfilDeportistaService service, UsuarioService usuarioService) {
        this.service = service;
        this.usuarioService = usuarioService;
    }

    /**
     * Estado, facturacion, asistencia y evolucion tecnica. Cada seccion llega null si el rol no tiene su permiso
     * (ver_factura, ver_asistencia, ver_evaluaciones); los pagos recientes ademas piden ver_pago.
     */
    @GetMapping("/{id}/resumen")
    @RequiresPermission("ver_deportista")
    public ResponseEntity<?> resumen(@PathVariable Integer id) {
        try {
            Set<String> permisos = usuarioService.obtenerPermisos(AuthenticatedUserUtil.getCurrentUserId().intValue());
            return ResponseEntity.ok(service.paraClub(id, permisos));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }
}
