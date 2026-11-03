package com.varcal.cheermanager.Controller.Caja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Service.Caja.CajaService;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** Caja diaria. Permisos del club (V15): ver_caja, cerrar_caja, reabrir_caja. */
@RestController
@RequestMapping("/api/caja")
public class CajaController {

    public record CierreRequest(LocalDate fecha,
            BigDecimal efectivoContado, String nota) {
    }

    public record ReaperturaRequest(String motivo) {
    }

    private final CajaService caja;

    public CajaController(CajaService caja) {
        this.caja = caja;
    }

    @GetMapping("/resumen")
    @RequiresPermission("ver_caja")
    public CajaService.Resumen resumen(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return caja.resumen(fecha);
    }

    @GetMapping("/cierres")
    @RequiresPermission("ver_caja")
    public List<CajaService.CierreResumen> cierres(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return caja.listarCierres(desde, hasta);
    }

    @PostMapping("/cierres")
    @RequiresPermission("cerrar_caja")
    public CajaService.Resumen cerrar(@RequestBody CierreRequest req) {
        return caja.cerrar(req.fecha(), req.efectivoContado(), req.nota());
    }

    @PostMapping("/cierres/{fecha}/reabrir")
    @RequiresPermission("reabrir_caja")
    public CajaService.Resumen reabrir(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestBody ReaperturaRequest req) {
        return caja.reabrir(fecha, req.motivo());
    }
}
