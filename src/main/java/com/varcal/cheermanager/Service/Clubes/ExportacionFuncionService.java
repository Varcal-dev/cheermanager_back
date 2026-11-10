package com.varcal.cheermanager.Service.Clubes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Descarga en autoservicio de los datos de una funcion del plan (un ZIP con un CSV por tabla). Siempre esta
 * disponible, tambien cuando la funcion esta en solo lectura o bloqueada: los datos son del club y nunca se borran.
 *
 * Solo se exportan tablas de la lista TABLAS (nombres fijos, nunca del usuario) y solo las filas del club. Si una tabla
 * no existe o no tiene club_id en esta base, se omite y se avisa en el LEEME.txt del ZIP.
 */
@Service
public class ExportacionFuncionService {

    /** Funcion -> tablas con sus datos. Una lista vacia significa que la funcion no guarda datos propios. */
    public static final Map<String, List<String>> TABLAS = new LinkedHashMap<>();
    static {
        TABLAS.put("RUBRICA_RUTINAS", List.of("evaluaciones_rutina", "registro_sub_criterio", "registro_driver_sub_criterio",
                "registro_deduccion_driver"));
        TABLAS.put("ROSTER", List.of("rosters_competencia", "roster_integrantes"));
        TABLAS.put("VIAJES", List.of("viajes_competencia", "viaje_participantes", "viaje_abonos"));
        TABLAS.put("RANKING", List.of());
        TABLAS.put("GESTION_ENTRENADORES", List.of("certificaciones_entrenador", "horas_entrenador"));
        TABLAS.put("TIENDA", List.of("categorias_producto", "productos", "producto_variantes", "movimientos_inventario",
                "ventas", "detalles_venta"));
        TABLAS.put("CONVENIOS", List.of("convenios"));
        TABLAS.put("RECORDATORIOS_MORA", List.of("recordatorios_config", "recordatorios_envios"));
        TABLAS.put("AUDITORIA", List.of("auditoria"));
        TABLAS.put("CONCILIACION_BANCARIA", List.of());
        TABLAS.put("MULTISEDE", List.of("sedes", "espacios", "asignaciones_espacio"));
    }

    private final JdbcTemplate jdbc;

    public ExportacionFuncionService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------ puro (probado sin base de datos)

    /** CSV con BOM UTF-8 (Excel lo abre bien) y saltos CRLF. */
    public static byte[] csv(List<String> columnas, List<Map<String, Object>> filas) {
        StringBuilder sb = new StringBuilder("﻿");
        sb.append(String.join(",", columnas.stream().map(ExportacionFuncionService::celda).toList())).append("\r\n");
        for (Map<String, Object> f : filas) {
            List<String> vals = new ArrayList<>();
            for (String c : columnas) {
                vals.add(celda(f.get(c)));
            }
            sb.append(String.join(",", vals)).append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    static String celda(Object v) {
        if (v == null) {
            return "";
        }
        String s = v instanceof Boolean b ? (b ? "1" : "0") : v.toString();
        // neutraliza formulas de Excel (=, +, -, @) al abrir el CSV
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0 && !(v instanceof Number)) {
            s = "'" + s;
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // ------------------------------------------------------------------ con base de datos

    /** ZIP con los datos de la funcion para el club indicado. Lanza si la funcion no existe o no guarda datos. */
    public byte[] zip(int clubId, String funcion) throws IOException {
        List<String> tablas = TABLAS.get(funcion);
        if (tablas == null) {
            throw new RuntimeException("Función desconocida");
        }
        if (tablas.isEmpty()) {
            throw new RuntimeException("Esta función no guarda datos propios: se calcula a partir de otros módulos que "
                    + "siguen incluidos en tu plan (por ejemplo, los resultados de eventos).");
        }
        StringBuilder leeme = new StringBuilder("Datos de la función " + funcion + " exportados el " + SuscripcionService.hoy()
                + ".\r\nCada archivo es una tabla en CSV (UTF-8). Si un archivo falta, esa tabla no tiene datos en tu base.\r\n");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            for (String t : tablas) {
                Integer conClub = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = "
                        + "DATABASE() AND table_name = ? AND column_name = 'club_id'", Integer.class, t);
                if (conClub == null || conClub == 0) {
                    leeme.append("- ").append(t).append(": omitida (no disponible en esta versión).\r\n");
                    continue;
                }
                List<String> columnas = jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE "
                        + "table_schema = DATABASE() AND table_name = ? ORDER BY ordinal_position", String.class, t);
                List<Map<String, Object>> filas = jdbc.queryForList("SELECT * FROM `" + t + "` WHERE club_id = ?", clubId);
                zip.putNextEntry(new ZipEntry(t + ".csv"));
                zip.write(csv(columnas, filas));
                zip.closeEntry();
                leeme.append("- ").append(t).append(": ").append(filas.size()).append(" filas.\r\n");
            }
            zip.putNextEntry(new ZipEntry("LEEME.txt"));
            zip.write(leeme.toString().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    public static LocalDate hoy() {
        return SuscripcionService.hoy();
    }
}
