package com.varcal.cheermanager.Controller.Notificaciones;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.DTO.Notificaciones.RecordatorioConfigDTO;
import com.varcal.cheermanager.DTO.Notificaciones.RecordatorioMoraRequestDTO;
import com.varcal.cheermanager.Service.Financiero.FacturaService;
import com.varcal.cheermanager.Service.Notificaciones.RecordatorioMoraService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/recordatorios-mora")
public class RecordatorioMoraController {

    private final RecordatorioMoraService service;

    public RecordatorioMoraController(RecordatorioMoraService service) {
        this.service = service;
    }

    // Que canales estan listos en el servidor (el sistema siempre; correo con SMTP; WhatsApp con la API de Meta).
    @GetMapping("/canales")
    @RequiresPermission("gestionar_recordatorios_mora")
    public ResponseEntity<?> canales() {
        return ResponseEntity.ok(service.canalesDisponibles());
    }

    @GetMapping("/config")
    @RequiresPermission("gestionar_recordatorios_mora")
    public ResponseEntity<?> config() {
        var c = service.obtenerConfig();
        return ResponseEntity.ok(Map.of(
                "automatico", c.getAutomatico(),
                "sistema", c.getCanalSistema(),
                "correo", c.getCanalCorreo(),
                "whatsapp", c.getCanalWhatsapp(),
                "diasEntreAvisos", c.getDiasEntreAvisos()));
    }

    @PutMapping("/config")
    @RequiresPermission("gestionar_recordatorios_mora")
    public ResponseEntity<?> guardarConfig(@RequestBody RecordatorioConfigDTO dto) {
        try {
            var c = service.actualizarConfig(dto);
            return ResponseEntity.ok(Map.of(
                    "automatico", c.getAutomatico(),
                    "sistema", c.getCanalSistema(),
                    "correo", c.getCanalCorreo(),
                    "whatsapp", c.getCanalWhatsapp(),
                    "diasEntreAvisos", c.getDiasEntreAvisos()));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    // Vista previa: a quien se avisaria y por donde, sin enviar ni registrar nada.
    @PostMapping("/previsualizar")
    @RequiresPermission("gestionar_recordatorios_mora")
    public ResponseEntity<?> previsualizar(@RequestBody RecordatorioMoraRequestDTO dto) {
        try {
            return ResponseEntity.ok(service.previsualizar(dto));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @PostMapping("/enviar")
    @RequiresPermission("gestionar_recordatorios_mora")
    public ResponseEntity<?> enviar(@RequestBody RecordatorioMoraRequestDTO dto) {
        try {
            return ResponseEntity.ok(service.enviar(dto, FacturaService.usuarioActualId()));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @GetMapping("/historial")
    @RequiresPermission("gestionar_recordatorios_mora")
    public ResponseEntity<?> historial(@RequestParam(defaultValue = "100") int limite) {
        return ResponseEntity.ok(service.historial(limite));
    }

    // Verifica el SMTP: envia un correo de prueba.
    @PostMapping("/probar-correo")
    @RequiresPermission("gestionar_recordatorios_mora")
    public ResponseEntity<?> probarCorreo(@RequestBody Map<String, String> body) {
        try {
            service.probarCorreo(body.get("email"));
            return ResponseEntity.ok(Map.of("enviado", true));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    // 400 dato invalido, 409 no disponible/regla, 500 fallo al enviar.
    private ResponseEntity<String> error(RuntimeException e) {
        int status = e instanceof IllegalArgumentException ? 400 : e instanceof IllegalStateException ? 409 : 500;
        return ResponseEntity.status(status).body(e.getMessage());
    }
}
