package com.varcal.cheermanager.Controller.Funcionalidad;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.DTO.Funcionalidad.AuditoriaDTO;
import com.varcal.cheermanager.Service.Funcionalidad.AuditoriaService;
import com.varcal.cheermanager.Utils.RequiresPermission;

// Reutiliza "ver_dash_administracion": ya existe, ya está seedeado, y es el
// mismo permiso que protege la sección "Administración" del sidebar donde
// vive el enlace a esta pantalla (ver nav-config.ts). No tiene sentido pedir
// un permiso nuevo para algo que ya está detrás de esa puerta.
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    @Autowired
    private AuditoriaService auditoriaService;

    // Filtros combinables, todos opcionales. Paginado vía Pageable estándar
    // de Spring (?page=0&size=20&sort=fecha,desc).
    @GetMapping
    @RequiresPermission("ver_dash_administracion")
    public ResponseEntity<Page<AuditoriaDTO>> listar(
            @RequestParam(required = false) String tabla,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) Integer usuarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        return ResponseEntity.ok(auditoriaService.listar(tabla, accion, usuarioId, desde, hasta, q, pageable));
    }

    // Alimenta el filtro "Tabla" en el front con las tablas que realmente
    // tienen actividad registrada, en vez de una lista fija.
    @GetMapping("/tablas")
    @RequiresPermission("ver_dash_administracion")
    public ResponseEntity<List<String>> tablas() {
        return ResponseEntity.ok(auditoriaService.listarTablasAfectadas());
    }
}