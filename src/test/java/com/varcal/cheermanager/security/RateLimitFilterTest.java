package com.varcal.cheermanager.security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

/** SEC-01: limite de intentos por IP en login / recuperacion de clave. */
class RateLimitFilterTest {

    private final AtomicLong ahora = new AtomicLong(1_000_000L);

    /** login 3/5min, forgot 2/1h, reset 2/1h para que las pruebas sean cortas. */
    private RateLimitFilter filtro(boolean activo) {
        return new RateLimitFilter(activo, 3, 300_000, 2, 3_600_000, 2, 3_600_000, ahora::get);
    }

    private MockHttpServletRequest post(String ruta, String ip) {
        MockHttpServletRequest r = new MockHttpServletRequest("POST", ruta);
        r.setRemoteAddr(ip);
        return r;
    }

    /** Ejecuta el filtro; la "app" responde con el status indicado. Devuelve la respuesta y cuenta llamadas a la cadena. */
    private MockHttpServletResponse ejecutar(RateLimitFilter f, MockHttpServletRequest req, int statusApp, AtomicInteger llamadas)
            throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = (rq, rs) -> {
            llamadas.incrementAndGet();
            ((MockHttpServletResponse) rs).setStatus(statusApp);
        };
        f.doFilter(req, resp, chain);
        return resp;
    }

    @Test
    void loginBloqueaTrasLosIntentosFallidosPermitidos() throws Exception {
        RateLimitFilter f = filtro(true);
        AtomicInteger llamadas = new AtomicInteger();
        for (int i = 0; i < 3; i++) {
            assertEquals(401, ejecutar(f, post("/api/login", "9.9.9.9"), 401, llamadas).getStatus());
        }
        MockHttpServletResponse r = ejecutar(f, post("/api/login", "9.9.9.9"), 401, llamadas);
        assertEquals(429, r.getStatus());
        assertEquals(3, llamadas.get(), "el 4.o intento no debe llegar a la aplicacion");
        assertTrue(Long.parseLong(r.getHeader("Retry-After")) > 0);
        assertTrue(r.getContentAsString().contains("Demasiados intentos"));
        assertTrue(r.getContentType().startsWith("application/json"));
    }

    @Test
    void loginExitosoNoCuentaParaElLimite() throws Exception {
        RateLimitFilter f = filtro(true);
        AtomicInteger llamadas = new AtomicInteger();
        for (int i = 0; i < 30; i++) {
            assertEquals(200, ejecutar(f, post("/api/login", "9.9.9.9"), 200, llamadas).getStatus());
        }
        assertEquals(30, llamadas.get());
    }

    @Test
    void forgotPasswordCuentaTodasLasSolicitudes() throws Exception {
        RateLimitFilter f = filtro(true);
        AtomicInteger llamadas = new AtomicInteger();
        assertEquals(200, ejecutar(f, post("/api/forgot-password", "9.9.9.9"), 200, llamadas).getStatus());
        assertEquals(200, ejecutar(f, post("/api/forgot-password", "9.9.9.9"), 200, llamadas).getStatus());
        assertEquals(429, ejecutar(f, post("/api/forgot-password", "9.9.9.9"), 200, llamadas).getStatus());
        assertEquals(2, llamadas.get());
    }

    @Test
    void resetPasswordCuentaSoloLosFallidos() throws Exception {
        RateLimitFilter f = filtro(true);
        AtomicInteger llamadas = new AtomicInteger();
        ejecutar(f, post("/api/reset-password", "9.9.9.9"), 200, llamadas);
        ejecutar(f, post("/api/reset-password", "9.9.9.9"), 200, llamadas);
        ejecutar(f, post("/api/reset-password", "9.9.9.9"), 200, llamadas);
        assertEquals(400, ejecutar(f, post("/api/reset-password", "9.9.9.9"), 400, llamadas).getStatus());
        assertEquals(400, ejecutar(f, post("/api/reset-password", "9.9.9.9"), 400, llamadas).getStatus());
        assertEquals(429, ejecutar(f, post("/api/reset-password", "9.9.9.9"), 400, llamadas).getStatus());
    }

    @Test
    void elBloqueoExpiraConLaVentana() throws Exception {
        RateLimitFilter f = filtro(true);
        AtomicInteger llamadas = new AtomicInteger();
        for (int i = 0; i < 3; i++) ejecutar(f, post("/api/login", "9.9.9.9"), 401, llamadas);
        assertEquals(429, ejecutar(f, post("/api/login", "9.9.9.9"), 401, llamadas).getStatus());
        ahora.addAndGet(300_000);
        assertEquals(401, ejecutar(f, post("/api/login", "9.9.9.9"), 401, llamadas).getStatus());
    }

    @Test
    void unaIpBloqueadaNoAfectaAOtra() throws Exception {
        RateLimitFilter f = filtro(true);
        AtomicInteger llamadas = new AtomicInteger();
        for (int i = 0; i < 4; i++) ejecutar(f, post("/api/login", "9.9.9.9"), 401, llamadas);
        assertEquals(401, ejecutar(f, post("/api/login", "8.8.8.8"), 401, llamadas).getStatus());
    }

    @Test
    void soloAplicaAPostEnLasRutasProtegidas() throws Exception {
        RateLimitFilter f = filtro(true);
        AtomicInteger llamadas = new AtomicInteger();
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest get = new MockHttpServletRequest("GET", "/api/login");
            get.setRemoteAddr("9.9.9.9");
            assertEquals(401, ejecutar(f, get, 401, llamadas).getStatus());
            assertEquals(401, ejecutar(f, post("/api/deportistas", "9.9.9.9"), 401, llamadas).getStatus());
        }
        assertEquals(20, llamadas.get());
    }

    @Test
    void desactivadoNoLimitaNada() throws Exception {
        RateLimitFilter f = filtro(false);
        AtomicInteger llamadas = new AtomicInteger();
        for (int i = 0; i < 20; i++) {
            assertEquals(401, ejecutar(f, post("/api/login", "9.9.9.9"), 401, llamadas).getStatus());
        }
    }

    @Test
    void detrasDeProxyLocalSeUsaLaUltimaEntradaDeXForwardedFor() throws Exception {
        RateLimitFilter f = filtro(true);
        AtomicInteger llamadas = new AtomicInteger();
        // El cliente falsifica la primera entrada en cada intento; el proxy siempre anade la IP real al final.
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest r = post("/api/login", "127.0.0.1");
            r.addHeader("X-Forwarded-For", "10.0." + i + ".1, 203.0.113.7");
            ejecutar(f, r, 401, llamadas);
        }
        MockHttpServletRequest r = post("/api/login", "127.0.0.1");
        r.addHeader("X-Forwarded-For", "6.6.6.6, 203.0.113.7");
        assertEquals(429, ejecutar(f, r, 401, llamadas).getStatus());
        // Otro cliente real detras del mismo proxy no se ve afectado.
        MockHttpServletRequest otro = post("/api/login", "127.0.0.1");
        otro.addHeader("X-Forwarded-For", "198.51.100.9");
        assertEquals(401, ejecutar(f, otro, 401, llamadas).getStatus());
    }

    @Test
    void xForwardedForSeIgnoraSiQuienConectaEsUnaIpPublica() throws Exception {
        MockHttpServletRequest r = post("/api/login", "203.0.113.50");
        r.addHeader("X-Forwarded-For", "1.2.3.4");
        assertEquals("203.0.113.50", RateLimitFilter.ipCliente(r));
    }

    @Test
    void reconoceProxiesLocalesYPrivados() {
        assertTrue(RateLimitFilter.esProxyConfiable("127.0.0.1"));
        assertTrue(RateLimitFilter.esProxyConfiable("172.18.0.1"));
        assertTrue(RateLimitFilter.esProxyConfiable("10.1.2.3"));
        assertTrue(RateLimitFilter.esProxyConfiable("192.168.1.5"));
        assertTrue(RateLimitFilter.esProxyConfiable("::1"));
        assertTrue(RateLimitFilter.esProxyConfiable("fd12:3456::1"));
        assertFalse(RateLimitFilter.esProxyConfiable("8.8.8.8"));
        assertFalse(RateLimitFilter.esProxyConfiable(null));
    }
}
