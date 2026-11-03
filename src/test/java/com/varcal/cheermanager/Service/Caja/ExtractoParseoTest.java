package com.varcal.cheermanager.Service.Caja;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ExtractoParseoTest {

    private static BigDecimal m(String s) {
        return ExtractoParseo.monto(s);
    }

    private static void rechaza(Runnable r) {
        try {
            r.run();
        } catch (IllegalArgumentException e) {
            return;
        }
        throw new AssertionError("deberia haber fallado");
    }

    @Test
    void montosColombianos() {
        assertEquals(0, new BigDecimal("1234567.89").compareTo(m("1.234.567,89")));
        assertEquals(0, new BigDecimal("1500").compareTo(m("1.500")));
        assertEquals(0, new BigDecimal("150000").compareTo(m("$ 150.000")));
        assertEquals(0, new BigDecimal("1234567.89").compareTo(m("1,234,567.89")));
        assertEquals(0, new BigDecimal("12.5").compareTo(m("12,5")));
        assertEquals(0, new BigDecimal("150000").compareTo(m("150000")));
        assertEquals(0, new BigDecimal("80000").compareTo(m("80000.00")));
    }

    @Test
    void montosNegativos() {
        assertEquals(-1, m("-150000").signum());
        assertEquals(-1, m("(150.000)").signum());
        assertEquals(-1, m("150000-").signum());
    }

    @Test
    void montosInvalidos() {
        rechaza(() -> m(""));
        rechaza(() -> m("abc"));
        rechaza(() -> m("12a"));
        rechaza(() -> m("."));
        rechaza(() -> m(null));
    }

    @Test
    void fechas() {
        assertEquals(LocalDate.of(2026, 10, 30), ExtractoParseo.fecha("2026-10-30"));
        assertEquals(LocalDate.of(2026, 10, 30), ExtractoParseo.fecha("30/10/2026"));
        assertEquals(LocalDate.of(2026, 10, 5), ExtractoParseo.fecha("5/10/2026"));
        assertEquals(LocalDate.of(2026, 10, 30), ExtractoParseo.fecha("2026-10-30 14:05:00"));
        assertEquals(LocalDate.of(2026, 10, 30), ExtractoParseo.fecha("2026-10-30T14:05:00"));
        rechaza(() -> ExtractoParseo.fecha("31/02/2026"));
        rechaza(() -> ExtractoParseo.fecha("ayer"));
        rechaza(() -> ExtractoParseo.fecha(""));
    }

    @Test
    void encabezados() {
        assertEquals("fecha", ExtractoParseo.nombreColumna("Fecha Movimiento"));
        assertEquals("valor", ExtractoParseo.nombreColumna("Crédito"));
        assertEquals("descripcion", ExtractoParseo.nombreColumna("Descripción"));
        assertEquals("referencia", ExtractoParseo.nombreColumna("Nº Documento"));
        assertTrue(ExtractoParseo.nombreColumna("Saldo").equals("saldo"));
    }
}
