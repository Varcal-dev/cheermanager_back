package com.varcal.cheermanager.Controller.Portal;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.Service.Consentimientos.AutorizacionPortalService;
import com.varcal.cheermanager.Utils.AuthenticatedUserUtil;
import com.varcal.cheermanager.Utils.RequiresPermission;

import jakarta.servlet.http.HttpServletRequest;

/** Autorizaciones de tratamiento de datos que el acudiente firma desde el portal (L3). */
@RestController
@RequestMapping("/api/portal")
public class PortalAutorizacionesController {

    private final AutorizacionPortalService service;

    public PortalAutorizacionesController(AutorizacionPortalService service) {
        this.service = service;
    }

    @GetMapping("/autorizaciones")
    @RequiresPermission("acceder_portal")
    public List<AutorizacionPortalService.Item> mias() {
        return service.mias(AuthenticatedUserUtil.getCurrentUserId().intValue());
    }

    /** Body: { "tratamiento": true, "salud": true, "imagen": false }. */
    @PostMapping("/deportistas/{id}/autorizaciones")
    @RequiresPermission("acceder_portal")
    public ResponseEntity<?> firmar(@PathVariable Integer id, @RequestBody AutorizacionPortalService.Firma body,
            HttpServletRequest request) {
        try {
            return ResponseEntity.status(201).body(service.firmar(
                    AuthenticatedUserUtil.getCurrentUserId().intValue(), id, body, ipDe(request)));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private static String ipDe(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return xff != null && !xff.isBlank() ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}
