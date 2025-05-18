package com.varcal.cheermanager.Controller.Financiero;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Models.Financiero.Descuento;
import com.varcal.cheermanager.Service.Financiero.DescuentoService;

@RestController
@RequestMapping("/api/descuentos")
public class DescuentoController {
    @Autowired
    private DescuentoService service;

    @GetMapping
    public List<Descuento> listar() {
        return service.obtenerTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Descuento> obtener(@PathVariable Integer id) {
        return service.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Descuento crear(@RequestBody Descuento descuento) {
        return service.crear(descuento);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Descuento> actualizar(@PathVariable Integer id, @RequestBody Descuento nuevo) {
        return service.actualizar(id, nuevo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (service.eliminar(id)) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
