package com.varcal.cheermanager.Controller.Notificaciones;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.Models.Notificaciones.Comunicado;
import com.varcal.cheermanager.Models.Notificaciones.ComunicadoEnvio;
import com.varcal.cheermanager.Service.Notificaciones.CalendarioService;
import com.varcal.cheermanager.Service.Notificaciones.ComunicadoService;
import com.varcal.cheermanager.Utils.AuthenticatedUserUtil;
import com.varcal.cheermanager.Utils.RequiresPermission;

/** C7: comunicados a grupos y calendarios descargables (.ics). */
@RestController
@RequestMapping("/api/comunicados")
public class ComunicadoController {

    private final ComunicadoService service;
    private final CalendarioService calendario;

    public ComunicadoController(ComunicadoService service, CalendarioService calendario) {
        this.service = service;
        this.calendario = calendario;
    }

    /** ?grupoId=3 (sin grupoId = todo el club): cuantas personas lo recibirian. */
    @GetMapping("/destinatarios")
    @RequiresPermission("ver_comunicados")
    public ResponseEntity<?> destinatarios(@RequestParam(required = false) Integer grupoId) {
        return ejecutar(200, () -> service.previsualizar(grupoId));
    }

    @PostMapping
    @RequiresPermission("enviar_comunicados")
    public ResponseEntity<?> enviar(@RequestBody ComunicadoService.Solicitud body) {
        return ejecutar(201, () -> service.enviar(body, AuthenticatedUserUtil.getCurrentUserId().intValue()));
    }

    @GetMapping
    @RequiresPermission("ver_comunicados")
    public List<Comunicado> historial() {
        return service.historial();
    }

    @GetMapping("/{id}/envios")
    @RequiresPermission("ver_comunicados")
    public List<ComunicadoEnvio> detalle(@PathVariable Integer id) {
        return service.detalle(id);
    }

    @GetMapping("/calendario/grupo/{grupoId}")
    @RequiresPermission("ver_comunicados")
    public ResponseEntity<?> calendarioGrupo(@PathVariable Integer grupoId) {
        try {
            return ics(calendario.nombreArchivoGrupo(grupoId), calendario.calendarioGrupo(grupoId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/calendario/eventos")
    @RequiresPermission("ver_comunicados")
    public ResponseEntity<?> calendarioEventos() {
        return ics("eventos-y-competencias.ics", calendario.calendarioEventos());
    }

    private static ResponseEntity<byte[]> ics(String nombre, String contenido) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.parseMediaType("text/calendar;charset=UTF-8"))
                .body(contenido.getBytes(StandardCharsets.UTF_8));
    }

    private static ResponseEntity<?> ejecutar(int ok, Supplier<Object> accion) {
        try {
            return ResponseEntity.status(ok).body(accion.get());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        }
    }
}
