package com.varcal.cheermanager.Controller.Persona;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.Models.Personas.DocumentoDeportista.Tipo;
import com.varcal.cheermanager.Service.Persona.DocumentoDeportistaService;
import com.varcal.cheermanager.Service.Persona.DocumentoDeportistaService.Alertas;
import com.varcal.cheermanager.Service.Persona.DocumentoDeportistaService.Documento;
import com.varcal.cheermanager.Service.Persona.DocumentoDeportistaService.Solicitud;
import com.varcal.cheermanager.Utils.AuthenticatedUserUtil;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** C4: vencimientos de certificado medico, EPS, seguro y licencia. */
@RestController
@RequestMapping("/api/documentos-deportista")
public class DocumentoDeportistaController {

    private final DocumentoDeportistaService service;

    public DocumentoDeportistaController(DocumentoDeportistaService service) {
        this.service = service;
    }

    /** ?dias=30&requeridos=CERTIFICADO_MEDICO,EPS&todos=false */
    @GetMapping("/alertas")
    @RequiresPermission("ver_deportista")
    public ResponseEntity<?> alertas(@RequestParam(defaultValue = "30") int dias,
            @RequestParam(required = false) List<Tipo> requeridos,
            @RequestParam(defaultValue = "false") boolean todos) {
        try {
            Set<Tipo> req = requeridos == null ? null
                    : requeridos.isEmpty() ? EnumSet.noneOf(Tipo.class) : EnumSet.copyOf(requeridos);
            Alertas a = service.alertas(dias, req, todos);
            return ResponseEntity.ok(a);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/deportista/{id}")
    @RequiresPermission("ver_deportista")
    public List<Documento> historial(@PathVariable Integer id) {
        return service.historial(id);
    }

    @PostMapping("/deportista/{id}")
    @RequiresPermission("modificar_deportista")
    public ResponseEntity<?> registrar(@PathVariable Integer id, @RequestBody Solicitud body) {
        try {
            return ResponseEntity.status(201).body(service.registrar(id, body,
                    AuthenticatedUserUtil.getCurrentUserId().intValue()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
