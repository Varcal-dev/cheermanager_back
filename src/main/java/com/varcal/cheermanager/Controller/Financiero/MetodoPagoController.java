package com.varcal.cheermanager.Controller.Financiero;

import com.varcal.cheermanager.Utils.SinPermisoEspecifico;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Models.Financiero.MetodoPago;
import com.varcal.cheermanager.repository.Financiero.MetodoPagoRepository;

// Catálogo de solo lectura. Mismo caso que TipoPagoController: el repository
// ya existía pero no había forma de listarlo desde el frontend.
//
// IMPORTANTE: si la tabla metodos_pago está vacía, este endpoint devuelve []
// y el selector del frontend quedará vacío. Necesitas insertar al menos un
// registro (ver instrucciones en el mensaje de Claude).
@RestController
@RequestMapping("/api/metodos-pago")
public class MetodoPagoController {

    @Autowired
    private MetodoPagoRepository metodoPagoRepository;

    @GetMapping
    @SinPermisoEspecifico("catalogo de lectura")
    public List<MetodoPago> listar() {
        return metodoPagoRepository.findAll();
    }
}