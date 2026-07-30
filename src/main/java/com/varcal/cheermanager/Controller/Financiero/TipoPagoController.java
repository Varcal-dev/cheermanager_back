package com.varcal.cheermanager.Controller.Financiero;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Models.Financiero.TipoPago;
import com.varcal.cheermanager.repository.Financiero.TipoPagoRepository;

// Catálogo de solo lectura. TipoPagoRepository ya existía (lo agregó
// PagoService en su momento), pero no tenía Controller — el frontend no
// tenía forma de pedir la lista de tipos de pago para el selector de
// "Registrar Pago". Misma convención que DiaSemanaController: sin
// @RequiresPermission, es un catálogo de referencia.
//
// IMPORTANTE: si la tabla tipos_pago está vacía, este endpoint devuelve []
// y el selector del frontend quedará vacío. Necesitas insertar al menos un
// registro (ver instrucciones en el mensaje de Claude).
@RestController
@RequestMapping("/api/tipos-pago")
public class TipoPagoController {

    @Autowired
    private TipoPagoRepository tipoPagoRepository;

    @GetMapping
    public List<TipoPago> listar() {
        return tipoPagoRepository.findAll();
    }
}