package com.varcal.cheermanager.Controller.Clubes;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.Service.Clubes.InteresadoService;
import com.varcal.cheermanager.Utils.RequiresPermission;
import com.varcal.cheermanager.Utils.SinPermisoEspecifico;

import jakarta.servlet.http.HttpServletRequest;

/** C6: pre-inscripcion publica (POST sin sesion, por slug del club) y su gestion por el club. */
@RestController
public class InteresadoController {

    private final InteresadoService service;

    public InteresadoController(InteresadoService service) {
        this.service = service;
    }

    @PostMapping("/api/public/clubes/{slug}/interesados")
    @SinPermisoEspecifico("endpoint publico del formulario de pre-inscripcion (abierto en SecurityConfig); valida, limita por IP y registra el consentimiento")
    public ResponseEntity<?> registrar(@PathVariable String slug, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            service.registrarPublico(slug, body, ip(request));
            // Misma respuesta si se guardo o ya estaba: no revela quien ya se registro.
            return ResponseEntity.status(201).body(Map.of("ok", true));
        } catch (InteresadoService.TooManyRequests e) {
            return ResponseEntity.status(429).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/api/interesados")
    @RequiresPermission("ver_interesados")
    public InteresadoService.Resumen listar() {
        return service.listar();
    }

    /** Body: { "estado": "PRUEBA_AGENDADA", "grupoId": 3, "fechaPrueba": "2026-11-20", "notas": "..." } (todo opcional). */
    @PutMapping("/api/interesados/{id}")
    @RequiresPermission("gestionar_interesados")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody InteresadoService.Actualizacion body) {
        try {
            return ResponseEntity.ok(service.actualizar(id, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private static String ip(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
