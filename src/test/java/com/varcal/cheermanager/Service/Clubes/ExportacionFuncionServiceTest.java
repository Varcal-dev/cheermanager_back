package com.varcal.cheermanager.Service.Clubes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class ExportacionFuncionServiceTest {

    @Test
    void csvEscapaComasComillasYSaltos() {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("nombre", "Ana, \"la capitana\"");
        f.put("nota", "linea1\nlinea2");
        f.put("monto", 1500);
        f.put("activo", true);
        f.put("vacio", null);
        String csv = new String(ExportacionFuncionService.csv(List.of("nombre", "nota", "monto", "activo", "vacio"), List.of(f)),
                StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\uFEFFnombre,nota,monto,activo,vacio\r\n"));
        assertTrue(csv.contains("\"Ana, \"\"la capitana\"\"\",\"linea1\nlinea2\",1500,1,\r\n"));
    }

    @Test
    void csvNeutralizaFormulasDeExcel() {
        assertEquals("'=SUMA(A1)", ExportacionFuncionService.celda("=SUMA(A1)"));
        assertEquals("'@cmd", ExportacionFuncionService.celda("@cmd"));
        assertEquals("-5", ExportacionFuncionService.celda(-5)); // un numero negativo no es formula
        assertEquals("", ExportacionFuncionService.celda(null));
    }

    @Test
    void todaFuncionConPuertaTieneSuListaDeTablas() {
        Set<String> conReglas = FuncionesService.REGLAS.stream().map(FuncionesService.Regla::funcion).collect(Collectors.toSet());
        for (String f : conReglas) {
            assertTrue(ExportacionFuncionService.TABLAS.containsKey(f), "falta exportacion para " + f);
        }
        // y no hay exportaciones de funciones que no existen
        for (String f : ExportacionFuncionService.TABLAS.keySet()) {
            assertTrue(conReglas.contains(f) || f.equals("CONCILIACION_BANCARIA"), "funcion sin regla: " + f);
        }
    }

    @Test
    void lasTablasNuevasExistenEnSusMigraciones() throws Exception {
        String sql = "";
        for (String m : List.of("V31__entrenadores_certificaciones_y_horas", "V35__roster_competencia", "V37__sedes_y_espacios",
                "V38__viajes_competencia")) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("db/migration/" + m + ".sql")) {
                assertTrue(in != null, "no se encuentra " + m);
                sql += new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        for (String f : List.of("ROSTER", "VIAJES", "MULTISEDE", "GESTION_ENTRENADORES")) {
            for (String t : ExportacionFuncionService.TABLAS.get(f)) {
                assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS " + t + " ") || sql.contains("CREATE TABLE " + t + " ")
                        || sql.contains("CREATE TABLE IF NOT EXISTS " + t + "\n") || sql.contains(" " + t + " ("), "tabla " + t);
            }
        }
    }
}
