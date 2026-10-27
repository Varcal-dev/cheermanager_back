package com.varcal.cheermanager.Controller.Notificaciones;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Utils.AuthenticatedUserUtil;
import com.varcal.cheermanager.Utils.RequiresPermission;
import com.varcal.cheermanager.repository.Funcionalidad.NotificacionRepository;

/** Avisos del usuario del portal (deportista o acudiente). Cada uno ve SOLO los suyos. */
@RestController
@RequestMapping("/api/portal/notificaciones")
public class PortalNotificacionesController {

    public record NotificacionItem(Integer id, String mensaje, LocalDateTime fecha, boolean vista) {
    }

    public record ListaNotificaciones(long noVistas, List<NotificacionItem> items) {
    }

    private final NotificacionRepository repo;

    public PortalNotificacionesController(NotificacionRepository repo) {
        this.repo = repo;
    }

    private Integer usuarioId() {
        return AuthenticatedUserUtil.getCurrentUserId().intValue();
    }

    @GetMapping
    @RequiresPermission("acceder_portal")
    public ListaNotificaciones mias() {
        Integer uid = usuarioId();
        List<NotificacionItem> items = repo.findTop50ByUsuarioIdOrderByFechaDesc(uid).stream()
                .map(n -> new NotificacionItem(n.getId(), n.getMensaje(), n.getFecha(), Boolean.TRUE.equals(n.getVista())))
                .toList();
        return new ListaNotificaciones(repo.countByUsuarioIdAndVistaFalse(uid), items);
    }

    @PatchMapping("/{id}/vista")
    @RequiresPermission("acceder_portal")
    @Transactional
    public ResponseEntity<Void> marcarVista(@PathVariable Integer id) {
        return repo.findByIdAndUsuarioId(id, usuarioId()).map(n -> {
            n.setVista(true);
            repo.save(n);
            return ResponseEntity.<Void>noContent().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/marcar-todas")
    @RequiresPermission("acceder_portal")
    @Transactional
    public ResponseEntity<Void> marcarTodas() {
        repo.marcarTodasVistas(usuarioId());
        return ResponseEntity.noContent().build();
    }
}
