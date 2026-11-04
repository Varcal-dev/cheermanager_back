package com.varcal.cheermanager.Controller.Legal;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Models.Legal.AceptacionLegal;
import com.varcal.cheermanager.Service.Legal.LegalService;
import com.varcal.cheermanager.Utils.AuthenticatedUserUtil;
import com.varcal.cheermanager.Utils.RequiresPermission;
import com.varcal.cheermanager.Utils.SinPermisoEspecifico;

import jakarta.servlet.http.HttpServletRequest;

/** Estado y aceptacion de terminos y politica del club (L1). */
@RestController
@RequestMapping("/api/legal")
public class LegalController {

    private final LegalService legalService;

    public LegalController(LegalService legalService) {
        this.legalService = legalService;
    }

    @GetMapping("/estado")
    @SinPermisoEspecifico("lo consulta cualquier usuario del club para saber si hay documentos pendientes; solo devuelve versiones y si estan aceptados")
    public LegalService.Estado estado() {
        return legalService.estado();
    }

    @GetMapping("/historial")
    @RequiresPermission("aceptar_terminos")
    public java.util.List<AceptacionLegal> historial() {
        return legalService.historial();
    }

    /** Body: { "terminos": true, "politica": true }. */
    @PostMapping("/aceptar")
    @RequiresPermission("aceptar_terminos")
    public ResponseEntity<?> aceptar(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            Set<String> confirmados = new HashSet<>();
            if (Boolean.TRUE.equals(body.get("terminos"))) confirmados.add(LegalService.TERMINOS);
            if (Boolean.TRUE.equals(body.get("politica"))) confirmados.add(LegalService.POLITICA);
            return ResponseEntity.ok(legalService.aceptar(AuthenticatedUserUtil.getCurrentUserId().intValue(), ipDe(request), confirmados));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** Detras de Caddy la IP real llega en X-Forwarded-For (Caddy lo reescribe; no es suplantable desde fuera). */
    static String ipDe(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
