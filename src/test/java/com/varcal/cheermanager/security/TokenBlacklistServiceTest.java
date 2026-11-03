package com.varcal.cheermanager.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class TokenBlacklistServiceTest {

    /** Almacen en memoria que hace de "base de datos" y sobrevive a un "reinicio" (otra instancia del servicio). */
    static class AlmacenFalso implements TokenBlacklistService.Almacen {
        final Map<String, Long> filas = new HashMap<>();
        boolean fallar;

        public void guardar(String hash, long expiraMs) {
            if (fallar) throw new RuntimeException("base caida");
            filas.put(hash, expiraMs);
        }

        public Map<String, Long> cargarVigentes(long ahoraMs) {
            if (fallar) throw new RuntimeException("base caida");
            Map<String, Long> r = new HashMap<>();
            filas.forEach((h, e) -> { if (e > ahoraMs) r.put(h, e); });
            return r;
        }

        public void purgar(long ahoraMs) {
            filas.values().removeIf(e -> e <= ahoraMs);
        }
    }

    private static final String TOKEN = "aaa.bbb.ccc";

    @Test
    void unTokenRevocadoSiguiendoRevocadoDespuesDeReiniciar() {
        AtomicLong reloj = new AtomicLong(1_000);
        AlmacenFalso bd = new AlmacenFalso();
        TokenBlacklistService antes = new TokenBlacklistService(bd, reloj::get);
        antes.blacklistToken(TOKEN, new Date(10_000));
        assertTrue(antes.isBlacklisted(TOKEN));

        TokenBlacklistService despues = new TokenBlacklistService(bd, reloj::get); // "reinicio"
        assertFalse(despues.isBlacklisted(TOKEN));
        despues.cargar();
        assertTrue(despues.isBlacklisted(TOKEN));
        assertFalse(despues.isBlacklisted("otro.token.x"));
    }

    @Test
    void nuncaGuardaElTokenEnClaro() {
        AlmacenFalso bd = new AlmacenFalso();
        TokenBlacklistService s = new TokenBlacklistService(bd, () -> 0L);
        s.blacklistToken(TOKEN, new Date(5_000));
        assertEquals(1, bd.filas.size());
        String clave = bd.filas.keySet().iterator().next();
        assertEquals(64, clave.length());
        assertFalse(clave.contains("aaa"));
        assertEquals(TokenBlacklistService.hash(TOKEN), clave);
    }

    @Test
    void unTokenVencidoYaNoCuentaYSeOlvida() {
        AtomicLong reloj = new AtomicLong(1_000);
        AlmacenFalso bd = new AlmacenFalso();
        TokenBlacklistService s = new TokenBlacklistService(bd, reloj::get);
        s.blacklistToken(TOKEN, new Date(2_000));
        assertTrue(s.isBlacklisted(TOKEN));
        reloj.set(3_000);
        assertFalse(s.isBlacklisted(TOKEN));
        assertEquals(0, s.tamano());
    }

    @Test
    void laTareaHorariaPurgaMemoriaYBase() {
        AtomicLong reloj = new AtomicLong(1_000);
        AlmacenFalso bd = new AlmacenFalso();
        TokenBlacklistService s = new TokenBlacklistService(bd, reloj::get);
        s.blacklistToken("t1.a.b", new Date(2_000));
        s.blacklistToken("t2.a.b", new Date(9_000));
        reloj.set(5_000);
        s.purgarVencidos();
        assertEquals(1, s.tamano());
        assertEquals(1, bd.filas.size());
        assertTrue(s.isBlacklisted("t2.a.b"));
    }

    @Test
    void siLaBaseFallaLaRevocacionValeEnMemoria() {
        AlmacenFalso bd = new AlmacenFalso();
        bd.fallar = true;
        TokenBlacklistService s = new TokenBlacklistService(bd, () -> 0L);
        s.cargar(); // no debe lanzar
        s.blacklistToken(TOKEN, new Date(5_000)); // no debe lanzar
        assertTrue(s.isBlacklisted(TOKEN));
        s.purgarVencidos(); // no debe lanzar
    }

    @Test
    void modoSoloMemoriaYEntradasNulas() {
        TokenBlacklistService s = new TokenBlacklistService();
        s.blacklistToken(null, new Date());
        s.blacklistToken(TOKEN, null);
        assertFalse(s.isBlacklisted(null));
        assertFalse(s.isBlacklisted(TOKEN));
        s.blacklistToken(TOKEN, new Date(System.currentTimeMillis() + 60_000));
        assertTrue(s.isBlacklisted(TOKEN));
    }
}
