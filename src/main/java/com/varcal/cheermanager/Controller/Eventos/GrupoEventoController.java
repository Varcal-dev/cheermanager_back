package com.varcal.cheermanager.Controller.Eventos;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.DTO.Eventos.GrupoEventoDTO;
import com.varcal.cheermanager.DTO.Eventos.GrupoEventoResponseDTO;
import com.varcal.cheermanager.Service.Eventos.GrupoEventoService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/grupos-evento")
public class GrupoEventoController {

    @Autowired
    private GrupoEventoService grupoEventoService;

    @PostMapping
    @RequiresPermission("modificar_evento")
    public ResponseEntity<?> inscribir(@RequestBody GrupoEventoDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(grupoEventoService.inscribir(dto));
        } catch (RuntimeException e) {
            // "ya está inscrito" es conflicto (409), "no encontrado" es 404 —
            // se distingue por el mensaje para no complicar el service con
            // tipos de excepción propios todavía.
            HttpStatus status = e.getMessage() != null && e.getMessage().contains("ya está inscrito")
                    ? HttpStatus.CONFLICT
                    : HttpStatus.NOT_FOUND;
            return ResponseEntity.status(status).body(e.getMessage());
        }
    }

    @GetMapping("/evento/{eventoId}")
    @RequiresPermission("ver_evento")
    public ResponseEntity<List<GrupoEventoResponseDTO>> listarPorEvento(@PathVariable Integer eventoId) {
        return ResponseEntity.ok(grupoEventoService.listarPorEvento(eventoId));
    }

    // Historial de competencias de un grupo — línea de tiempo del equipo.
    @GetMapping("/grupo/{grupoEntrenamientoId}")
    @RequiresPermission("ver_evento")
    public ResponseEntity<List<GrupoEventoResponseDTO>> listarPorGrupo(@PathVariable Integer grupoEntrenamientoId) {
        return ResponseEntity.ok(grupoEventoService.listarPorGrupo(grupoEntrenamientoId));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("modificar_evento")
    public ResponseEntity<?> desinscribir(@PathVariable Integer id) {
        try {
            grupoEventoService.desinscribir(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}