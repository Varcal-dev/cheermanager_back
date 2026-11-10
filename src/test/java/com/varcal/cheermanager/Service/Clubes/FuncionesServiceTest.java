package com.varcal.cheermanager.Service.Clubes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class FuncionesServiceTest {

    @Test
    void rutasConPuertaSeReconocen() {
        assertEquals("VIAJES", FuncionesService.reglaDe("/api/viajes", "GET").orElseThrow().funcion());
        assertEquals("VIAJES", FuncionesService.reglaDe("/api/viajes/12/cerrar", "POST").orElseThrow().funcion());
        assertEquals("TIENDA", FuncionesService.reglaDe("/api/ventas/5", "GET").orElseThrow().funcion());
        assertEquals("MULTISEDE", FuncionesService.reglaDe("/api/sedes/3/espacios", "PUT").orElseThrow().funcion());
    }

    @Test
    void rutasDelNucleoYParecidasNoSeFrenan() {
        assertTrue(FuncionesService.reglaDe("/api/deportistas", "GET").isEmpty());
        assertTrue(FuncionesService.reglaDe("/api/eventos", "POST").isEmpty());
        assertTrue(FuncionesService.reglaDe("/api/rubrica-config", "GET").isEmpty());
        assertTrue(FuncionesService.reglaDe("/api/lesiones", "POST").isEmpty());
        // un prefijo solo coincide por segmento completo
        assertTrue(FuncionesService.reglaDe("/api/viajesX", "GET").isEmpty());
        assertTrue(FuncionesService.reglaDe("/api/ventas-externas", "GET").isEmpty());
    }

    @Test
    void conveniosYRecordatoriosSoloFrenanLaEscritura() {
        assertTrue(FuncionesService.reglaDe("/api/convenios", "GET").isEmpty());
        assertEquals("CONVENIOS", FuncionesService.reglaDe("/api/convenios", "POST").orElseThrow().funcion());
        assertEquals("CONVENIOS", FuncionesService.reglaDe("/api/convenios/4", "DELETE").orElseThrow().funcion());
        assertTrue(FuncionesService.reglaDe("/api/recordatorios-mora", "GET").isEmpty());
        assertEquals("RECORDATORIOS_MORA", FuncionesService.reglaDe("/api/recordatorios-mora/enviar", "POST").orElseThrow().funcion());
    }

    @Test
    void preflightCorsNuncaSeFrena() {
        assertTrue(FuncionesService.reglaDe("/api/viajes", "OPTIONS").isEmpty());
        assertTrue(FuncionesService.reglaDe(null, "GET").isEmpty());
    }

    @Test
    void lasExcepcionesDelClubAgreganOQuitan() {
        Set<String> plan = Set.of("ROSTER", "TIENDA");
        Set<String> r = FuncionesService.resolver(plan, Map.of("VIAJES", true, "TIENDA", false));
        assertEquals(Set.of("ROSTER", "VIAJES"), r);
        assertEquals(plan, FuncionesService.resolver(plan, Map.of()));
    }

    @Test
    void habilitadasIncluyeSegunTodasOCodigos() {
        assertTrue(new FuncionesService.Habilitadas(true, Set.of()).incluye("VIAJES"));
        assertTrue(new FuncionesService.Habilitadas(false, Set.of("VIAJES")).incluye("VIAJES"));
        assertFalse(new FuncionesService.Habilitadas(false, Set.of("VIAJES")).incluye("ROSTER"));
    }

    /** Si una regla apunta a una funcion que la migracion no siembra, el plan nunca la trae y se bloquea a todos. */
    @Test
    void todaFuncionDeUnaReglaExisteEnLaMigracion() throws Exception {
        String sql;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("db/migration/V39__funciones_por_plan.sql")) {
            assertTrue(in != null, "no se encuentra V39");
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        for (FuncionesService.Regla r : FuncionesService.REGLAS) {
            assertTrue(sql.contains("'" + r.funcion() + "',"), "V39 no siembra " + r.funcion());
            assertTrue(sql.contains("UNION ALL SELECT '" + r.funcion() + "'") || sql.contains("SELECT '" + r.funcion() + "' "),
                    "V39 no asigna " + r.funcion() + " a ningun plan");
        }
    }
}
