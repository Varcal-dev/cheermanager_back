package com.varcal.cheermanager.Controller.Clubes;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Service.Clubes.PlataformaService;
import com.varcal.cheermanager.Utils.RequiresPermission;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Trabajo del superadmin DENTRO de un club (usuarios, claves, acceso de soporte). Solo permiso gestionar_clubes.
 * Los errores de negocio (RuntimeException) los traduce el manejador global igual que en ClubController.
 */
@RestController
@RequestMapping("/api/plataforma/clubes/{clubId}")
public class PlataformaController {

    private final PlataformaService service;

    public PlataformaController(PlataformaService service) {
        this.service = service;
    }

    @GetMapping("/usuarios")
    @RequiresPermission("gestionar_clubes")
    public List<PlataformaService.UsuarioClub> usuarios(@PathVariable Integer clubId) {
        return service.listarUsuarios(clubId);
    }

    @GetMapping("/roles")
    @RequiresPermission("gestionar_clubes")
    public List<PlataformaService.RolClub> roles(@PathVariable Integer clubId) {
        return service.listarRoles(clubId);
    }

    /** Crea un usuario operativo con una clave temporal que se devuelve UNA sola vez. */
    @PostMapping("/usuarios")
    @RequiresPermission("gestionar_clubes")
    public ResponseEntity<PlataformaService.UsuarioCreado> crearUsuario(@PathVariable Integer clubId,
            @RequestBody PlataformaService.NuevoUsuarioRequest request) {
        return ResponseEntity.status(201).body(service.crearUsuario(clubId, request));
    }

    @PostMapping("/usuarios/{usuarioId}/clave-temporal")
    @RequiresPermission("gestionar_clubes")
    public PlataformaService.ClaveGenerada claveTemporal(@PathVariable Integer clubId, @PathVariable Integer usuarioId) {
        return service.claveTemporal(clubId, usuarioId);
    }

    /** Token para entrar al club como su Administrador (soporte). Queda en el log de acceso del club. */
    @PostMapping("/acceso")
    @RequiresPermission("gestionar_clubes")
    public PlataformaService.AccesoSoporte acceso(@PathVariable Integer clubId, HttpServletRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return service.accesoSoporte(clubId, email, request.getRemoteAddr());
    }
}
