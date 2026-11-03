package com.varcal.cheermanager.Service.Caja;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class CajaReglasTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 1);

    private static String error(java.util.function.Supplier<String> s) {
        try {
            s.get();
        } catch (RuntimeException e) {
            return e.getMessage();
        }
        return null;
    }

    private static BigDecimal b(String s) {
        return new BigDecimal(s);
    }

    @Test
    void cierreExactoSinNota() {
        assertEquals(null, CajaReglas.validarCierre(HOY, HOY, b("100000"), b("100000.00"), null, null));
    }

    @Test
    void diferenciaExigeNota() {
        String e = error(() -> CajaReglas.validarCierre(HOY, HOY, b("90000"), b("100000"), "  ", null));
        assertEquals(true, e != null && e.contains("nota"));
        assertEquals("Faltaron 10 mil", CajaReglas.validarCierre(HOY, HOY, b("90000"), b("100000"), " Faltaron 10 mil ", null));
    }

    @Test
    void signoDeLaDiferencia() {
        assertEquals(-1, CajaReglas.diferencia(b("100"), b("90")).signum());
        assertEquals(1, CajaReglas.diferencia(b("100"), b("110")).signum());
    }

    @Test
    void noSePuedeCerrarFuturoNiDosVecesNiSinConteo() {
        assertEquals(true, error(() -> CajaReglas.validarCierre(HOY.plusDays(1), HOY, b("0"), b("0"), null, null)).contains("futura"));
        assertEquals(true, error(() -> CajaReglas.validarCierre(HOY, HOY, b("0"), b("0"), null, "CERRADA")).contains("ya esta cerrada"));
        assertEquals(true, error(() -> CajaReglas.validarCierre(HOY, HOY, null, b("0"), null, null)).contains("contado"));
        assertEquals(true, error(() -> CajaReglas.validarCierre(HOY, HOY, b("-1"), b("0"), null, null)).contains("contado"));
        assertEquals(null, error(() -> CajaReglas.validarCierre(HOY, HOY, b("0"), b("0"), null, "REABIERTA")));
    }

    @Test
    void notaDemasiadoLarga() {
        assertEquals(true, error(() -> CajaReglas.validarCierre(HOY, HOY, b("0"), b("0"), "x".repeat(301), null)).contains("300"));
    }
}
