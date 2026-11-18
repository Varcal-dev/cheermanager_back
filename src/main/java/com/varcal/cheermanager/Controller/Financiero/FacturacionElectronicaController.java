package com.varcal.cheermanager.Controller.Financiero;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Models.Financiero.DocumentoElectronico;
import com.varcal.cheermanager.Service.Financiero.FacturacionConfigService;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** Configuracion fiscal y documentos de la facturacion electronica DIAN (V45). */
@RestController
@RequestMapping("/api/facturacion-electronica")
public class FacturacionElectronicaController {

    private final FacturacionConfigService service;

    public FacturacionElectronicaController(FacturacionConfigService service) {
        this.service = service;
    }

    @GetMapping("/config")
    @RequiresPermission("gestionar_facturacion_electronica")
    public FacturacionConfigService.Estado config() {
        return service.obtener();
    }

    @PutMapping("/config")
    @RequiresPermission("gestionar_facturacion_electronica")
    public FacturacionConfigService.Estado guardar(@RequestBody FacturacionConfigService.ConfigDTO dto) {
        return service.guardar(dto);
    }

    @GetMapping("/documentos")
    @RequiresPermission("gestionar_facturacion_electronica")
    public List<DocumentoElectronico> documentos() {
        return service.ultimosDocumentos();
    }
}
