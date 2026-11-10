package com.varcal.cheermanager.Service.Clubes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class PaquetesServiceTest {

    final LocalDate hoy = LocalDate.of(2026, 11, 10);

    @Test
    void vigenciaDelPaquete() {
        assertTrue(PaquetesService.vigente(hoy, null, hoy));
        assertTrue(PaquetesService.vigente(hoy.minusDays(5), hoy, hoy)); // el ultimo dia aun cuenta
        assertFalse(PaquetesService.vigente(hoy.plusDays(1), null, hoy)); // aun no empieza
        assertFalse(PaquetesService.vigente(hoy.minusDays(30), hoy.minusDays(1), hoy)); // ya termino
        assertFalse(PaquetesService.vigente(null, null, hoy));
    }

    @Test
    void elTopeDePaquetesSeRespeta() {
        PaquetesService.validarTope(0, 2, 2);
        PaquetesService.validarTope(1, 1, 2);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> PaquetesService.validarTope(2, 1, 2));
        assertTrue(ex.getMessage().contains("2"));
        assertThrows(RuntimeException.class, () -> PaquetesService.validarTope(0, 1, 0)); // Enterprise: sin paquetes
        assertThrows(RuntimeException.class, () -> PaquetesService.validarTope(0, 0, 2)); // cantidad invalida
    }

    @Test
    void quitarUnPaqueteSoloSiLosActivosCaben() {
        PaquetesService.validarQueCaben(75, 0, 75); // justo
        PaquetesService.validarQueCaben(null, 0, 900); // ilimitado
        RuntimeException ex = assertThrows(RuntimeException.class, () -> PaquetesService.validarQueCaben(75, 0, 90));
        assertTrue(ex.getMessage().contains("90") && ex.getMessage().contains("75"));
        PaquetesService.validarQueCaben(75, 25, 100); // con un paquete aun cabe
    }

    @Test
    void laMigracionSiembraUnPaquetePorPlanYTopeCero() throws Exception {
        String sql;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("db/migration/V40__paquetes_de_deportistas.sql")) {
            assertTrue(in != null, "no se encuentra V40");
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(sql.contains("'Paquete +25 deportistas',  25,  34900"));
        assertTrue(sql.contains("'Paquete +50 deportistas',  50,  51900"));
        assertTrue(sql.contains("'Paquete +100 deportistas', 100, 72600"));
        assertTrue(sql.contains("max_paquetes_deportistas = 0 WHERE codigo IN ('enterprise', 'fundador')"));
        assertEquals(1, sql.split("CREATE TABLE IF NOT EXISTS clubes_paquetes_deportistas", -1).length - 1);
    }
}
