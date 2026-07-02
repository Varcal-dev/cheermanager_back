package com.varcal.cheermanager.Controller.Eventos;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.DTO.Eventos.PremioEventoDTO;
import com.varcal.cheermanager.Service.Eventos.PremioEventoService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/premios-evento")
public class PremioEventoController {

    @Autowired
    private PremioEventoService premioEventoService;

    @PostMapping
    @RequiresPermission("modificar_evento")
    public ResponseEntity<?> crear(@RequestBody PremioEventoDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(premioEventoService.crear(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/evento/{eventoId}")
    @RequiresPermission("ver_evento")
    public ResponseEntity<?> listarPorEvento(@PathVariable Integer eventoId) {
        try {
            return ResponseEntity.ok(premioEventoService.listarPorEvento(eventoId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    @RequiresPermission("modificar_evento")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody PremioEventoDTO dto) {
        try {
            return ResponseEntity.ok(premioEventoService.actualizar(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("modificar_evento")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            premioEventoService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}