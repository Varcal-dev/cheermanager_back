package com.varcal.cheermanager.Controller.Dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.DTO.Dashboard.ResumenAdministracionDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenEvaluacionDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenEventosDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenFinanzasDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenHorariosDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenInicioDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenOrganizacionDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenPersonasDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenTiendaDTO;
import com.varcal.cheermanager.Service.Dashboard.DashboardAdministracionService;
import com.varcal.cheermanager.Service.Dashboard.DashboardEvaluacionService;
import com.varcal.cheermanager.Service.Dashboard.DashboardEventosService;
import com.varcal.cheermanager.Service.Dashboard.DashboardFinanzasService;
import com.varcal.cheermanager.Service.Dashboard.DashboardHorariosService;
import com.varcal.cheermanager.Service.Dashboard.DashboardInicioService;
import com.varcal.cheermanager.Service.Dashboard.DashboardOrganizacionService;
import com.varcal.cheermanager.Service.Dashboard.DashboardPersonasService;
import com.varcal.cheermanager.Service.Dashboard.DashboardTiendaService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardHorariosService dashboardHorariosService;

    @Autowired
    private DashboardEvaluacionService dashboardEvaluacionService;

    @Autowired
    private DashboardPersonasService dashboardPersonasService;

    @Autowired
    private DashboardInicioService dashboardInicioService;

    @Autowired
    private DashboardOrganizacionService dashboardOrganizacionService;

    @Autowired
    private DashboardFinanzasService dashboardFinanzasService;

    @Autowired
    private DashboardEventosService dashboardEventosService;

    @Autowired
    private DashboardAdministracionService dashboardAdministracionService;

    @Autowired
    private DashboardTiendaService dashboardTiendaService;

    @GetMapping("/inicio")
    @RequiresPermission("ver_dash_inicio")
    public ResponseEntity<ResumenInicioDTO> inicio() {
        return ResponseEntity.ok(dashboardInicioService.obtenerResumen());
    }

    @GetMapping("/organizacion")
    @RequiresPermission("ver_dash_organizacion")
    public ResponseEntity<ResumenOrganizacionDTO> organizacion() {
        return ResponseEntity.ok(dashboardOrganizacionService.obtenerResumen());
    }

    // Deportistas + Entrenadores en un solo dashboard (ver_dash_personas ya
    // los agrupa así en el seed de permisos, no hay ver_dash_entrenadores
    // separado).
    @GetMapping("/personas")
    @RequiresPermission("ver_dash_personas")
    public ResponseEntity<ResumenPersonasDTO> personas(@RequestParam(defaultValue = "30") int diasAltasBajas) {
        return ResponseEntity.ok(dashboardPersonasService.obtenerResumen(diasAltasBajas));
    }

    // ?dias=30 por defecto — configurable porque "asistencia reciente" puede
    // significar distintas cosas según a quién se le muestre (una semana
    // para operación diaria, un mes para revisión con dirección técnica).
    @GetMapping("/horarios")
    @RequiresPermission("ver_dash_horarios")
    public ResponseEntity<ResumenHorariosDTO> horarios(@RequestParam(defaultValue = "30") int dias) {
        return ResponseEntity.ok(dashboardHorariosService.obtenerResumen(dias));
    }

    @GetMapping("/evaluacion")
    @RequiresPermission("ver_dash_evaluacion")
    public ResponseEntity<ResumenEvaluacionDTO> evaluacion() {
        return ResponseEntity.ok(dashboardEvaluacionService.obtenerResumen());
    }

    @GetMapping("/evaluacion/progresion/{grupoId}")
    @RequiresPermission("ver_dash_evaluacion")
    public ResponseEntity<?> progresionGrupo(@PathVariable Integer grupoId) {
        try {
            return ResponseEntity.ok(dashboardEvaluacionService.obtenerProgresion(grupoId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/finanzas")
    @RequiresPermission("ver_dash_finanzas")
    public ResponseEntity<ResumenFinanzasDTO> finanzas() {
        return ResponseEntity.ok(dashboardFinanzasService.obtenerResumen());
    }

    @GetMapping("/eventos")
    @RequiresPermission("ver_evento")
    public ResponseEntity<ResumenEventosDTO> eventos() {
        return ResponseEntity.ok(dashboardEventosService.obtenerResumen());
    }

    @GetMapping("/administracion")
    @RequiresPermission("ver_dash_administracion")
    public ResponseEntity<ResumenAdministracionDTO> administracion() {
        return ResponseEntity.ok(dashboardAdministracionService.obtenerResumen());
    }

    @GetMapping("/tienda")
    @RequiresPermission("ver_dash_tienda")
    public ResponseEntity<ResumenTiendaDTO> tienda() {
        return ResponseEntity.ok(dashboardTiendaService.obtenerResumen());
    }
}