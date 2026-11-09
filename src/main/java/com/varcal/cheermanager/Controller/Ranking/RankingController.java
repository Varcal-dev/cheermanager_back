package com.varcal.cheermanager.Controller.Ranking;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.Service.Ranking.RankingService;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** C12: ranking e historial de resultados (solo lectura) y ranking de habilidades aprendidas. */
@RestController
@RequestMapping("/api/ranking")
public class RankingController {

    private final RankingService service;

    public RankingController(RankingService service) {
        this.service = service;
    }

    @GetMapping("/temporadas")
    @RequiresPermission("ver_evento")
    public List<RankingService.Temporada_> temporadas() {
        return service.listarTemporadas();
    }

    /** Sin temporadaId usa la temporada activa. */
    @GetMapping("/temporada")
    @RequiresPermission("ver_evento")
    public ResponseEntity<?> temporada(@RequestParam(required = false) Integer temporadaId) {
        return ejecutar(() -> service.rankingTemporada(temporadaId));
    }

    @GetMapping("/deportista/{id}")
    @RequiresPermission("ver_deportista")
    public ResponseEntity<?> deportista(@PathVariable Integer id) {
        return ejecutar(() -> service.historialDeportista(id));
    }

    /** ?anio=2026&mes=10 (sin mes = todo el anio). */
    @GetMapping("/habilidades")
    @RequiresPermission("ver_evaluaciones")
    public ResponseEntity<?> habilidades(@RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes) {
        return ejecutar(() -> service.rankingHabilidades(anio, mes));
    }

    private static ResponseEntity<?> ejecutar(Supplier<Object> accion) {
        try {
            return ResponseEntity.ok(accion.get());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
