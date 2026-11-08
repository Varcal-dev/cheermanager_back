package com.varcal.cheermanager.Controller.Financiero;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.Service.Financiero.PedidoUniformeService;
import com.varcal.cheermanager.Utils.AuthenticatedUserUtil;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** C9: tallas por grupo y pedido de uniformes cobrado en cuotas. */
@RestController
@RequestMapping("/api/uniformes")
public class PedidoUniformeController {

    private final PedidoUniformeService servicio;

    public PedidoUniformeController(PedidoUniformeService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/tallas-grupo/{grupoId}")
    @RequiresPermission("ver_deportista")
    public ResponseEntity<?> tallas(@PathVariable Integer grupoId) {
        try {
            return ResponseEntity.ok(servicio.tallasDeGrupo(grupoId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/pedidos")
    @RequiresPermission("ver_factura")
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(servicio.listar());
    }

    @PostMapping("/pedidos")
    @RequiresPermission("crear_factura")
    public ResponseEntity<?> crear(@RequestBody PedidoUniformeService.Solicitud solicitud) {
        try {
            return ResponseEntity.status(201)
                    .body(servicio.crear(solicitud, AuthenticatedUserUtil.getCurrentUserId().intValue()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** Factura las cuotas del pedido (simular=true: solo vista previa). Repetible: omite a quien ya tiene sus cuotas. */
    @PostMapping("/pedidos/{id}/facturar")
    @RequiresPermission("crear_factura")
    public ResponseEntity<?> facturar(@PathVariable Integer id, @RequestParam(defaultValue = "false") boolean simular) {
        try {
            return ResponseEntity.ok(servicio.facturar(id, simular));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        }
    }
}
