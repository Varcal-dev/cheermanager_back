
package com.varcal.cheermanager.Controller.Financiero;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.DTO.Financiero.CobroEventoLoteDTO;
import com.varcal.cheermanager.Service.Financiero.CobroEventoService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/eventos/{eventoId}/cobro-lote")
public class CobroEventoController {

    private final CobroEventoService cobroEventoService;

    public CobroEventoController(CobroEventoService cobroEventoService) {
        this.cobroEventoService = cobroEventoService;
    }

    // Cobra el evento a todos los deportistas de los grupos inscritos (simular=true: solo vista previa).
    @PostMapping
    @RequiresPermission("crear_factura")
    public ResponseEntity<?> cobrar(@PathVariable Integer eventoId, @RequestBody CobroEventoLoteDTO dto) {
        try {
            return ResponseEntity.ok(cobroEventoService.cobrar(eventoId, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }
}