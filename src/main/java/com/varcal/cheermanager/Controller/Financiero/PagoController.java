package com.varcal.cheermanager.Controller.Financiero;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.DTO.Financiero.PagoDTO;
import com.varcal.cheermanager.Service.Financiero.PagoService;
import com.varcal.cheermanager.DTO.Financiero.PagoResponseDTO;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping
    public ResponseEntity<PagoResponseDTO> registrar(@RequestBody PagoDTO dto) {
        PagoResponseDTO pago = pagoService.registrarPago(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(pago);
    }

    @GetMapping
    public List<PagoResponseDTO> listar() {
        return pagoService.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PagoResponseDTO> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(pagoService.obtenerPorId(id));
    }

    // Historial de pagos de una factura específica (ej. pagos parciales de una mensualidad)
    @GetMapping("/factura/{facturaId}")
    public ResponseEntity<List<PagoResponseDTO>> listarPorFactura(@PathVariable Integer facturaId) {
        return ResponseEntity.ok(pagoService.listarPorFactura(facturaId));
    }

    // Historial financiero de una persona (todas sus facturas, todos sus pagos)
    @GetMapping("/persona/{personaId}")
    public ResponseEntity<List<PagoResponseDTO>> listarPorPersona(@PathVariable Integer personaId) {
        return ResponseEntity.ok(pagoService.listarPorPersona(personaId));
    }

    // Cuánto le falta pagar a una factura en este momento
    @GetMapping("/factura/{facturaId}/saldo")
    public ResponseEntity<Map<String, Object>> saldoPendiente(@PathVariable Integer facturaId) {
        BigDecimal saldo = pagoService.calcularSaldoPendiente(facturaId);
        return ResponseEntity.ok(Map.of("facturaId", facturaId, "saldoPendiente", saldo));
    }

    // Cambiar el estado de un pago ya registrado (ej. de "Pendiente" a "Pagado"
    // cuando se confirma una transferencia)
    @PatchMapping("/{id}/estado")
    public ResponseEntity<PagoResponseDTO> actualizarEstado(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String nuevoEstado = body.get("estado");
        return ResponseEntity.ok(pagoService.actualizarEstado(id, nuevoEstado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        pagoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}