package com.varcal.cheermanager.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

class CalendarioHabilTest {

    @Test
    void festivosOficiales2026() {
        Set<LocalDate> esperados = new TreeSet<>(Set.of(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 12), LocalDate.of(2026, 3, 23),
                LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 3), LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 18), LocalDate.of(2026, 6, 8), LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 6, 29), LocalDate.of(2026, 7, 20), LocalDate.of(2026, 8, 7),
                LocalDate.of(2026, 8, 17), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 16), LocalDate.of(2026, 12, 8), LocalDate.of(2026, 12, 25)));
        assertEquals(esperados, new TreeSet<>(CalendarioHabil.festivos(2026)));
    }

    @Test
    void primerDiaHabilSaltaFinesDeSemanaYFestivos() {
        // 1 nov 2026 es domingo y el 2 es festivo (Todos los Santos trasladado al lunes)
        assertEquals(LocalDate.of(2026, 11, 3), CalendarioHabil.primerDiaHabil(YearMonth.of(2026, 11)));
        // 1 ene 2027 es viernes festivo; 2 y 3 son fin de semana
        assertEquals(LocalDate.of(2027, 1, 4), CalendarioHabil.primerDiaHabil(YearMonth.of(2027, 1)));
        // dic 2026 empieza martes
        assertEquals(LocalDate.of(2026, 12, 1), CalendarioHabil.primerDiaHabil(YearMonth.of(2026, 12)));
    }

    @Test
    void esHabil() {
        assertTrue(CalendarioHabil.esHabil(LocalDate.of(2026, 10, 20)));   // martes normal
        assertFalse(CalendarioHabil.esHabil(LocalDate.of(2026, 10, 12)));  // festivo (lunes)
        assertFalse(CalendarioHabil.esHabil(LocalDate.of(2026, 10, 17)));  // sabado
    }
}
