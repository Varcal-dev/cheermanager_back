package com.varcal.cheermanager.security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

/** PERF-02: el cache con expiracion, invalidacion y generacion. Reloj falso: sin esperas reales. */
class CacheTtlTest {

    private final AtomicLong ahora = new AtomicLong(1_000_000L);
    private final CacheTtl<String, String> cache = new CacheTtl<>(30_000, 100, ahora::get);

    @Test
    void dentroDelTtlNoVuelveACargar() {
        AtomicInteger cargas = new AtomicInteger();
        for (int i = 0; i < 5; i++) {
            assertEquals("v", cache.obtener("a", () -> { cargas.incrementAndGet(); return "v"; }));
        }
        assertEquals(1, cargas.get());
    }

    @Test
    void alVencerElTtlVuelveACargar() {
        AtomicInteger cargas = new AtomicInteger();
        cache.obtener("a", () -> "v" + cargas.incrementAndGet());
        ahora.addAndGet(29_999);
        assertEquals("v1", cache.obtener("a", () -> "v" + cargas.incrementAndGet()));
        ahora.addAndGet(1);   // exactamente 30 s: ya vencio
        assertEquals("v2", cache.obtener("a", () -> "v" + cargas.incrementAndGet()));
    }

    @Test
    void unValorNuloNoSeGuarda() {
        AtomicInteger cargas = new AtomicInteger();
        assertNull(cache.obtener("a", () -> { cargas.incrementAndGet(); return null; }));
        assertNull(cache.obtener("a", () -> { cargas.incrementAndGet(); return null; }));
        assertEquals(2, cargas.get(), "lo inexistente se consulta siempre");
        assertEquals(0, cache.tamano());
        assertEquals("ya existe", cache.obtener("a", () -> "ya existe"));   // al aparecer, se ve al instante
    }

    @Test
    void invalidarUnaClaveSoloAfectaAEsa() {
        cache.obtener("a", () -> "1");
        cache.obtener("b", () -> "2");
        cache.invalidar("a");
        assertEquals("nuevo", cache.obtener("a", () -> "nuevo"));
        assertEquals("2", cache.obtener("b", () -> "otro"));
    }

    @Test
    void invalidarSiYInvalidarTodo() {
        cache.obtener("x1", () -> "1");
        cache.obtener("x2", () -> "2");
        cache.obtener("y1", () -> "3");
        cache.invalidarSi(k -> k.startsWith("x"));
        assertEquals("n", cache.obtener("x1", () -> "n"));
        assertEquals("3", cache.obtener("y1", () -> "otro"));
        cache.invalidarTodo();
        assertEquals(0, cache.tamano());
    }

    @Test
    void unaCargaIniciadaAntesDeInvalidarNoGuardaSuResultadoViejo() {
        // La carga lee el dato "viejo"; mientras tanto otro hilo cambia el dato e invalida.
        String devuelto = cache.obtener("a", () -> {
            cache.invalidar("a");   // el cambio ocurre en mitad de la carga
            return "viejo";
        });
        assertEquals("viejo", devuelto, "quien pidio recibe lo que leyo");
        assertEquals(0, cache.tamano(), "pero no queda guardado");
        assertEquals("nuevo", cache.obtener("a", () -> "nuevo"));
    }

    @Test
    void unaInvalidacionDeOtraClaveTambienDescartaLaCargaEnCurso() {
        // Conservador a proposito: cualquier invalidacion durante la carga impide guardar.
        cache.obtener("a", () -> { cache.invalidar("otra"); return "v"; });
        assertEquals(0, cache.tamano());
    }

    @Test
    void conElMaximoLleno_purgaLasVencidasYSiNoHaySitioNoGuarda() {
        CacheTtl<Integer, String> c = new CacheTtl<>(1_000, 3, ahora::get);
        for (int i = 0; i < 3; i++) { int k = i; c.obtener(k, () -> "v" + k); }
        assertEquals(3, c.tamano());

        c.obtener(99, () -> "no cabe");                 // lleno y ninguna vencida: no se guarda
        assertEquals(3, c.tamano());
        AtomicInteger cargas = new AtomicInteger();
        c.obtener(99, () -> { cargas.incrementAndGet(); return "no cabe"; });
        assertEquals(1, cargas.get(), "como no se guardo, se carga cada vez");

        ahora.addAndGet(1_000);                         // ahora las 3 estan vencidas
        c.obtener(99, () -> "cabe");
        assertEquals(1, c.tamano(), "purgo las vencidas y guardo la nueva");
        assertEquals("cabe", c.obtener(99, () -> "otro"));
    }

    @Test
    void parametrosInvalidosSeRechazan() {
        assertThrows(IllegalArgumentException.class, () -> new CacheTtl<String, String>(0, 10, ahora::get));
        assertThrows(IllegalArgumentException.class, () -> new CacheTtl<String, String>(10, 0, ahora::get));
    }

    @Test
    void variosHilosPidiendoLoMismoNuncaRompenNiDevuelvenOtraCosa() throws Exception {
        int hilos = 16;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Integer>> resultados = new ArrayList<>();
        for (int h = 0; h < hilos; h++) {
            resultados.add(pool.submit(() -> {
                salida.await();
                int errores = 0;
                for (int i = 0; i < 2_000; i++) {
                    String k = "k" + (i % 7);
                    String v = cache.obtener(k, () -> "valor-" + k);
                    if (!("valor-" + k).equals(v)) errores++;
                    if (i % 500 == 0) cache.invalidar(k);   // invalidaciones concurrentes
                }
                return errores;
            }));
        }
        salida.countDown();
        for (Future<Integer> f : resultados) assertEquals(0, f.get(30, TimeUnit.SECONDS));
        pool.shutdown();
    }
}
