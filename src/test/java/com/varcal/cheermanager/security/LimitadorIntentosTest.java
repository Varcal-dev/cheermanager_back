package com.varcal.cheermanager.security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class LimitadorIntentosTest {

    private final AtomicLong ahora = new AtomicLong(1_000_000L);

    @Test
    void permiteHastaElMaximoYLuegoBloquea() {
        LimitadorIntentos l = new LimitadorIntentos(3, 60_000, ahora::get);
        for (int i = 0; i < 3; i++) {
            assertEquals(0, l.segundosParaReintentar("1.1.1.1"));
            l.registrar("1.1.1.1");
        }
        assertTrue(l.segundosParaReintentar("1.1.1.1") > 0);
    }

    @Test
    void elBloqueoSeLevantaCuandoVenceLaVentana() {
        LimitadorIntentos l = new LimitadorIntentos(2, 60_000, ahora::get);
        l.registrar("a");
        l.registrar("a");
        assertEquals(60, l.segundosParaReintentar("a"));
        ahora.addAndGet(59_000);
        assertEquals(1, l.segundosParaReintentar("a"));
        ahora.addAndGet(1_000);
        assertEquals(0, l.segundosParaReintentar("a"));
    }

    @Test
    void cadaClaveTieneSuPropioContador() {
        LimitadorIntentos l = new LimitadorIntentos(1, 60_000, ahora::get);
        l.registrar("a");
        assertTrue(l.segundosParaReintentar("a") > 0);
        assertEquals(0, l.segundosParaReintentar("b"));
    }

    @Test
    void parametrosInvalidosSeRechazan() {
        assertThrows(IllegalArgumentException.class, () -> new LimitadorIntentos(0, 1000, ahora::get));
        assertThrows(IllegalArgumentException.class, () -> new LimitadorIntentos(1, 0, ahora::get));
    }
}
