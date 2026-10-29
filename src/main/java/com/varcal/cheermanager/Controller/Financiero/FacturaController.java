package com.varcal.cheermanager.Controller.Financiero;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.DTO.Comunes.PaginaDTO;
import com.varcal.cheermanager.DTO.Financiero.FacturaManualDTO;
import com.varcal.cheermanager.DTO.Financiero.FacturaResponseDTO;
import com.varcal.cheermanager.Service.Financiero.FacturaService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    private final FacturaService facturaService;

    public FacturaController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    // Cobro manual (uniforme, competencia, evento, otro). Las facturas de inscripcion y
    // mensualidad las genera el sistema al inscribir / en la facturacion mensual.
    @PostMapping
    @RequiresPermission("crear_factura")
    public ResponseEntity<FacturaResponseDTO> crearManual(@RequestBody FacturaManualDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facturaService.crearManual(dto));
    }

    @GetMapping
    @RequiresPermission("ver_factura")
    public List<FacturaResponseDTO> listar() {
        return facturaService.listarDTO();
    }

    // Listado para la pantalla: paginado y con filtros (el listado completo de abajo se conserva
    // para quien ya lo consuma, pero la pantalla de Facturas ya no lo usa).
    @GetMapping("/pagina")
    @RequiresPermission("ver_factura")
    public PaginaDTO<FacturaResponseDTO> pagina(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String concepto,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return facturaService.listarPagina(q, estado, concepto, desde, hasta, pagina, tamano);
    }

    // Totales de cartera en mora (tarjeta de alerta de la pantalla).
    @GetMapping("/resumen-cartera")
    @RequiresPermission("ver_factura")
    public Map<String, Object> resumenCartera() {
        return facturaService.resumenCartera();
    }

    // Cartera en mora: facturas vencidas con saldo pendiente (las mas antiguas primero).
    @GetMapping("/mora")
    @RequiresPermission("ver_factura")
    public List<FacturaResponseDTO> enMora() {
        return facturaService.listarEnMora();
    }

    @GetMapping("/{id}")
    @RequiresPermission("ver_factura")
    public ResponseEntity<FacturaResponseDTO> obtener(@PathVariable Integer id) {
        return facturaService.buscarDTOPorId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // Las facturas no se borran: se anulan con motivo (queda fecha y usuario).
    @PatchMapping("/{id}/anular")
    @RequiresPermission("anular_factura")
    public ResponseEntity<FacturaResponseDTO> anular(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(facturaService.anular(id, body.get("motivo")));
    }
}