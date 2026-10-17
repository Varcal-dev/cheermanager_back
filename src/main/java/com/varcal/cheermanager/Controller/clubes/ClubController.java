package com.varcal.cheermanager.Controller.Clubes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Models.Clubes.Club;
import com.varcal.cheermanager.Service.Clubes.ClubService;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** Administracion de clubes: solo el superadmin de la plataforma (permiso gestionar_clubes). */
@RestController
@RequestMapping("/api/clubes")
public class ClubController {

    public record ClubResponse(Integer id, String nombre, String slug, Boolean activo, LocalDateTime fechaCreacion) {
        static ClubResponse de(Club c) {
            return new ClubResponse(c.getId(), c.getNombre(), c.getSlug(), c.getActivo(), c.getFechaCreacion());
        }
    }

    private final ClubService clubService;

    public ClubController(ClubService clubService) {
        this.clubService = clubService;
    }

    @PostMapping
    @RequiresPermission("gestionar_clubes")
    public ResponseEntity<ClubResponse> crear(@RequestBody ClubService.CrearClubRequest request) {
        return ResponseEntity.status(201).body(ClubResponse.de(clubService.crearClub(request)));
    }

    @GetMapping
    @RequiresPermission("gestionar_clubes")
    public List<ClubResponse> listar() {
        return clubService.listar().stream().map(ClubResponse::de).toList();
    }

    @GetMapping("/{id}")
    @RequiresPermission("gestionar_clubes")
    public ClubResponse obtener(@PathVariable Integer id) {
        return ClubResponse.de(clubService.obtener(id));
    }

    /** Body: { "activo": false } — suspende o reactiva un club (sus usuarios pierden acceso de inmediato). */
    @PatchMapping("/{id}/activo")
    @RequiresPermission("gestionar_clubes")
    public ClubResponse cambiarActivo(@PathVariable Integer id, @RequestBody Map<String, Boolean> body) {
        Boolean activo = body.get("activo");
        if (activo == null) {
            throw new RuntimeException("Falta el campo 'activo'");
        }
        return ClubResponse.de(clubService.cambiarActivo(id, activo));
    }
}
