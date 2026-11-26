package com.varcal.cheermanager.Controller.Persona;

import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.config.ErrorPublico;
import com.varcal.cheermanager.Service.Auth.UsuarioService;
import com.varcal.cheermanager.Service.Perfil.PerfilCompetenciaSaludService;
import com.varcal.cheermanager.Service.Perfil.PerfilDeportistaService;
import com.varcal.cheermanager.Utils.AuthenticatedUserUtil;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** Perfil 360 del deportista para el personal del club (solo lectura). */
@RestController
@RequestMapping("/api/deportistas")
public class PerfilDeportistaController {

    private final PerfilDeportistaService service;
    private final PerfilCompetenciaSaludService competenciaSalud;
    private final UsuarioService usuarioService;

    public PerfilDeportistaController(PerfilDeportistaService service, PerfilCompetenciaSaludService competenciaSalud,
            UsuarioService usuarioService) {
        this.service = service;
        this.competenciaSalud = competenciaSalud;
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
            return ResponseEntity.status(404).body(Map.of("message", ErrorPublico.de(e)));
        }
    }

    /** Roster, resultados de su grupo y aptitud para el proximo evento. */
    @GetMapping("/{id}/resumen/competencia")
    @RequiresPermission("ver_evento")
    public ResponseEntity<?> competencia(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(competenciaSalud.competencia(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", ErrorPublico.de(e)));
        }
    }

    /** Detalle de salud (seguimientos e historial de lesiones): solo con ver_lesiones. */
    @GetMapping("/{id}/resumen/salud")
    @RequiresPermission("ver_lesiones")
    public ResponseEntity<?> salud(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(competenciaSalud.salud(id, true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("message", ErrorPublico.de(e)));
        }
    }
}
