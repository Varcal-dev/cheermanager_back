package com.varcal.cheermanager.Controller.Financiero;

import com.varcal.cheermanager.Utils.RequiresPermission;

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
import com.varcal.cheermanager.DTO.Financiero.FacturaResponseDTO;
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
    @RequiresPermission("crear_factura")
    public ResponseEntity<Factura> crear(@RequestBody FacturaDTO dto) {
        Factura factura = facturaService.crearFactura(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(factura);
    }

    // Antes devolvía List<Factura> (la entidad JPA cruda), lo que arrastraba
    // factura.inscripcion.deportista.historialGrupos.grupo.deportistas... y
    // caía en un ciclo infinito de serialización. Ahora devuelve un DTO
    // plano con solo los campos que el frontend necesita.
    @GetMapping
    @RequiresPermission("ver_factura")
    public List<FacturaResponseDTO> listar() {
        return facturaService.listarDTO();
    }

    @GetMapping("/{id}")
    @RequiresPermission("ver_factura")
    public ResponseEntity<FacturaResponseDTO> obtener(@PathVariable Integer id) {
        return facturaService.buscarDTOPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("eliminar_factura")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        facturaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
