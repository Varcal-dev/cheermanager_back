package com.varcal.cheermanager.Service.Evaluaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class EvaluacionReglasTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 1);

    private static void rechazaPuntaje(Integer p) {
        try {
            EvaluacionReglas.validarPuntaje(p);
        } catch (IllegalArgumentException e) {
            return;
        }
        throw new AssertionError("deberia rechazar " + p);
    }

    @Test
    void puntajeEntre0Y10() {
        EvaluacionReglas.validarPuntaje(0);
        EvaluacionReglas.validarPuntaje(10);
        rechazaPuntaje(-1);
        rechazaPuntaje(11);
        rechazaPuntaje(null);
    }

    @Test
    void fechaSinValorEsHoyYNoSePermiteFuturo() {
        assertEquals(HOY, EvaluacionReglas.validarFecha(null, HOY));
        assertEquals(HOY.minusDays(3), EvaluacionReglas.validarFecha(HOY.minusDays(3), HOY));
        assertEquals(HOY, EvaluacionReglas.validarFecha(HOY, HOY));
        try {
            EvaluacionReglas.validarFecha(HOY.plusDays(1), HOY);
        } catch (IllegalArgumentException e) {
            return;
        }
        throw new AssertionError("fecha futura aceptada");
    }

    @Test
    void lotes() {
        EvaluacionReglas.validarLote(1);
        EvaluacionReglas.validarLote(200);
        for (int n : new int[] { 0, 201 }) {
            try {
                EvaluacionReglas.validarLote(n);
            } catch (IllegalArgumentException e) {
                continue;
            }
            throw new AssertionError("lote " + n + " aceptado");
        }
    }

    @Test
    void busquedaYPaginas() {
        assertEquals(null, EvaluacionReglas.patronBusqueda(null));
        assertEquals(null, EvaluacionReglas.patronBusqueda("  %_ "));
        assertEquals("%maria lopez%", EvaluacionReglas.patronBusqueda(" Maria Lopez "));
        assertEquals(50, EvaluacionReglas.tamanoPagina(null));
        assertEquals(1, EvaluacionReglas.tamanoPagina(0));
        assertEquals(100, EvaluacionReglas.tamanoPagina(5000));
        assertEquals(20, EvaluacionReglas.tamanoPagina(20));
    }
}
