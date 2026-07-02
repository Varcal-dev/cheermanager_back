package com.varcal.cheermanager.Controller.Eventos;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.DTO.Eventos.EventoDTO;
import com.varcal.cheermanager.DTO.Eventos.EventoResponseDTO;
import com.varcal.cheermanager.Service.Eventos.EventoService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    @Autowired
    private EventoService eventoService;

    @GetMapping
    @RequiresPermission("ver_evento")
    public ResponseEntity<List<EventoResponseDTO>> listar() {
        return ResponseEntity.ok(eventoService.listarTodos());
    }

    // Calendario: eventos desde hoy en adelante, para dashboard o notificaciones.
    @GetMapping("/proximos")
    @RequiresPermission("ver_evento")
    public ResponseEntity<List<EventoResponseDTO>> proximos() {
        return ResponseEntity.ok(eventoService.proximos());
    }

    // Historial: eventos ya pasados, más recientes primero.
    @GetMapping("/historial")
    @RequiresPermission("ver_evento")
    public ResponseEntity<List<EventoResponseDTO>> historial() {
        return ResponseEntity.ok(eventoService.historial());
    }

    @GetMapping("/{id}")
    @RequiresPermission("ver_evento")
    public ResponseEntity<?> obtener(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(eventoService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @RequiresPermission("crear_evento")
    public ResponseEntity<?> crear(@RequestBody EventoDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(eventoService.crear(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @RequiresPermission("modificar_evento")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody EventoDTO dto) {
        try {
            return ResponseEntity.ok(eventoService.actualizar(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("eliminar_evento")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            eventoService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            // No siempre es 404 (puede ser conflicto por resultados ya cargados),
            // pero se devuelve el mensaje explicando la causa igual.
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }
}