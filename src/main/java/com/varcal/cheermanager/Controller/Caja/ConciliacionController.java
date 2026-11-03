package com.varcal.cheermanager.Controller.Caja;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.varcal.cheermanager.Service.Caja.ConciliacionService;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** Conciliacion de pagos no efectivo contra el extracto del banco. Permiso del club (V15): conciliar_pagos. */
@RestController
@RequestMapping("/api/conciliacion")
public class ConciliacionController {

    public record MarcarRequest(List<Integer> ids, boolean conciliado) {
    }

    public record ReferenciaRequest(String referencia) {
    }

    private final ConciliacionService conciliacion;

    public ConciliacionController(ConciliacionService conciliacion) {
        this.conciliacion = conciliacion;
    }

    @GetMapping("/pagos")
    @RequiresPermission("conciliar_pagos")
    public List<ConciliacionService.PagoFila> pagos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Boolean conciliado) {
        return conciliacion.listar(desde, hasta, conciliado);
    }

    @PostMapping("/marcar")
    @RequiresPermission("conciliar_pagos")
    public Map<String, Object> marcar(@RequestBody MarcarRequest req) {
        return Map.of("actualizados", conciliacion.marcar(req.ids(), req.conciliado()));
    }

    @PatchMapping("/pagos/{id}/referencia")
    @RequiresPermission("conciliar_pagos")
    public ConciliacionService.PagoFila referencia(@PathVariable Integer id, @RequestBody ReferenciaRequest req) {
        return conciliacion.actualizarReferencia(id, req.referencia());
    }

    /** Analiza un extracto CSV y devuelve sugerencias. No modifica ningun pago. */
    @PostMapping("/extracto")
    @RequiresPermission("conciliar_pagos")
    public ConciliacionService.InformeExtracto extracto(@RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "ventanaDias", defaultValue = "3") int ventanaDias) throws IOException {
        return conciliacion.analizarExtracto(archivo.getBytes(), ventanaDias);
    }
}
