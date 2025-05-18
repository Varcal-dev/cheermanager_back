package com.varcal.cheermanager.Controller.Financiero;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.DTO.Financiero.FacturaDTO;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.Service.Financiero.FacturaService;

@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    private final FacturaService facturaService;

    public FacturaController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @PostMapping
    public ResponseEntity<Factura> crear(@RequestBody FacturaDTO dto) {
        Factura factura = facturaService.crearFactura(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(factura);
    }

    @GetMapping
    public List<Factura> listar() {
        return facturaService.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Factura> obtener(@PathVariable Integer id) {
        return facturaService.buscarPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        facturaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
