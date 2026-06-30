package com.varcal.cheermanager.Controller.Funcionalidad;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.DTO.Funcionalidad.LogAccesoDTO;
import com.varcal.cheermanager.Service.Funcionalidad.LogAccesoService;
import com.varcal.cheermanager.Utils.RequiresPermission;

// NOTA: "ver_logs_acceso" es un permiso nuevo, no existía en tu tabla de
// permisos. Agrégalo (igual que los demás) y asígnalo al rol que corresponda
// (Admin, y quizás Tesorero si quieres que audite también). Sin ese permiso
// cargado en la BD, estos endpoints devolverán 403 para todos.
@RestController
@RequestMapping("/api/logs-acceso")
public class LogAccesoController {

    @Autowired
    private LogAccesoService logAccesoService;

    @GetMapping("/usuario/{usuarioId}")
    @RequiresPermission("ver_logs_acceso")
    public ResponseEntity<Page<LogAccesoDTO>> porUsuario(@PathVariable Integer usuarioId, Pageable pageable) {
        return ResponseEntity.ok(logAccesoService.listarPorUsuario(usuarioId, pageable));
    }

    // ej. /api/logs-acceso/por-accion?accion=LOGIN_FALLIDO
    @GetMapping("/por-accion")
    @RequiresPermission("ver_logs_acceso")
    public ResponseEntity<Page<LogAccesoDTO>> porAccion(@RequestParam String accion, Pageable pageable) {
        return ResponseEntity.ok(logAccesoService.listarPorAccion(accion, pageable));
    }

    @GetMapping("/rango")
    @RequiresPermission("ver_logs_acceso")
    public ResponseEntity<Page<LogAccesoDTO>> porRango(
            @RequestParam LocalDateTime desde,
            @RequestParam LocalDateTime hasta,
            Pageable pageable) {
        return ResponseEntity.ok(logAccesoService.listarPorRangoFechas(desde, hasta, pageable));
    }
}