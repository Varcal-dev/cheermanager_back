package com.varcal.cheermanager.Controller.Reportes;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Service.Clubes.SuscripcionService;
import com.varcal.cheermanager.Service.Importacion.ExportesService;
import com.varcal.cheermanager.Service.Reportes.AnalisisService;
import com.varcal.cheermanager.Service.Reportes.ReportesService;
import com.varcal.cheermanager.Utils.RequiresPermission;

/**
 * Reporte financiero consolidado y exportes CSV por rango de fechas (desde / hasta, AAAA-MM-DD, ambos opcionales:
 * por defecto los ultimos 12 meses). Todo es GET y de solo lectura: sigue disponible con la suscripcion vencida.
 * Finanzas (facturas, pagos, caja): generar_reporte_financiero. Asistencia y evaluaciones: exportar_listados.
 */
@RestController
@RequestMapping("/api")
public class ReportesController {

    private static final MediaType CSV = MediaType.parseMediaType("text/csv; charset=UTF-8");

    private final ReportesService reportes;
    private final ExportesService exportes;
    private final AnalisisService analisis;

    public ReportesController(ReportesService reportes, ExportesService exportes, AnalisisService analisis) {
        this.reportes = reportes;
        this.exportes = exportes;
        this.analisis = analisis;
    }

    /** Analisis historico mensual: altas, asistencia, retencion, evaluaciones, cobrado y deportistas en riesgo. */
    @GetMapping("/reportes/analisis")
    @RequiresPermission("ver_dash_organizacion")
    public AnalisisService.Analisis analisis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return analisis.analizar(desde, hasta, SuscripcionService.hoy());
    }

    @GetMapping("/reportes/financiero")
    @RequiresPermission("generar_reporte_financiero")
    public ReportesService.Financiero financiero(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reportes.financiero(desde, hasta, SuscripcionService.hoy());
    }

    @GetMapping("/exportes/facturas")
    @RequiresPermission("generar_reporte_financiero")
    public ResponseEntity<byte[]> facturas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return csv(exportes.facturas(desde, hasta), "facturas", desde, hasta);
    }

    @GetMapping("/exportes/pagos")
    @RequiresPermission("generar_reporte_financiero")
    public ResponseEntity<byte[]> pagos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return csv(exportes.pagos(desde, hasta), "pagos", desde, hasta);
    }

    @GetMapping("/exportes/cierres-caja")
    @RequiresPermission("generar_reporte_financiero")
    public ResponseEntity<byte[]> cierresCaja(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return csv(exportes.cierresCaja(desde, hasta), "cierres-caja", desde, hasta);
    }

    @GetMapping("/exportes/asistencia")
    @RequiresPermission("exportar_listados")
    public ResponseEntity<byte[]> asistencia(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return csv(exportes.asistencia(desde, hasta), "asistencia", desde, hasta);
    }

    @GetMapping("/exportes/evaluaciones")
    @RequiresPermission("exportar_listados")
    public ResponseEntity<byte[]> evaluaciones(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return csv(exportes.evaluaciones(desde, hasta), "evaluaciones", desde, hasta);
    }

    private static ResponseEntity<byte[]> csv(byte[] datos, String base, LocalDate desde, LocalDate hasta) {
        String nombre = base + (desde != null ? "-" + desde : "") + (hasta != null ? "-a-" + hasta : "") + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(CSV)
                .body(datos);
    }
}
