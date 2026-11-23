package com.varcal.cheermanager.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** IP real del cliente: una sola logica para limite de intentos, accesos, auditoria y aceptaciones legales. */
class ClienteIpTest {

    private static MockHttpServletRequest req(String remota, String xff) {
        MockHttpServletRequest r = new MockHttpServletRequest("POST", "/api/login");
        r.setRemoteAddr(remota);
        if (xff != null) r.addHeader("X-Forwarded-For", xff);
        return r;
    }

    @Test
    void sinEncabezadoUsaLaIpDirecta() {
        assertEquals("203.0.113.9", ClienteIp.de(req("203.0.113.9", null)));
    }

    @Test
    void siQuienConectaEsUnaIpPublicaElEncabezadoSeIgnora() {
        assertEquals("203.0.113.50", ClienteIp.de(req("203.0.113.50", "1.2.3.4")));
    }

    @Test
    void detrasDeProxyLocalUsaLaUltimaEntradaYNoLaPrimeraQueEscribeElCliente() {
        assertEquals("203.0.113.7", ClienteIp.de(req("127.0.0.1", "6.6.6.6, 10.0.0.1, 203.0.113.7")));
        assertEquals("198.51.100.9", ClienteIp.de(req("172.18.0.1", "198.51.100.9")));
    }

    @Test
    void siLaUltimaEntradaNoEsUnaIpValidaCaeALaIpDirecta() {
        assertEquals("127.0.0.1", ClienteIp.de(req("127.0.0.1", "1.2.3.4, no-soy-una-ip")));
        assertEquals("127.0.0.1", ClienteIp.de(req("127.0.0.1", "evil.example.com")));   // nombre de host: nunca se resuelve
        assertEquals("127.0.0.1", ClienteIp.de(req("127.0.0.1", "deadbeef")));
        assertEquals("127.0.0.1", ClienteIp.de(req("127.0.0.1", "999.1.1.1")));
        assertEquals("127.0.0.1", ClienteIp.de(req("127.0.0.1", "01.2.3.4")));
        assertEquals("127.0.0.1", ClienteIp.de(req("127.0.0.1", "   ")));
    }

    @Test
    void elResultadoSiempreCabeEnLasColumnasDe45Caracteres() {
        String larga = "1".repeat(500);
        assertTrue(ClienteIp.de(req("127.0.0.1", larga)).length() <= 45);
        assertTrue(ClienteIp.de(req("127.0.0.1", "2001:0db8:0000:0000:0000:ff00:0042:8329")).length() <= 45);
    }

    @Test
    void ipv6SeNormalizaYElMapeadoDeIpv4SeConvierte() {
        assertEquals("2001:db8:0:0:0:ff00:42:8329", ClienteIp.de(req("::1", "2001:DB8::ff00:42:8329")));
        assertEquals("1.2.3.4", ClienteIp.de(req("::1", "::ffff:1.2.3.4")));
    }

    @Test
    void reconoceProxiesLocalesPrivadosYDeHosting() {
        for (String ip : new String[] {"127.0.0.1", "172.18.0.1", "10.1.2.3", "192.168.1.5", "169.254.1.1",
                "::1", "fd12:3456::1", "100.64.0.1", "100.127.255.254"}) {
            assertTrue(ClienteIp.esProxyConfiable(ip), ip);
        }
        for (String ip : new String[] {"8.8.8.8", "100.63.255.255", "100.128.0.1", "172.32.0.1", "203.0.113.5", "no-ip", null}) {
            assertFalse(ClienteIp.esProxyConfiable(ip), String.valueOf(ip));
        }
    }

    @Test
    void sinNingunaIpDevuelveDesconocida() {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.setRemoteAddr("");
        assertEquals(ClienteIp.DESCONOCIDA, ClienteIp.de(r));
    }
}
