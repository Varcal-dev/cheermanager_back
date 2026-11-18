package com.varcal.cheermanager.Service.Clubes;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class WompiServiceTest {

    static final String SECRETO_EVENTOS = "test_events_secreto";
    static final String SECRETO_INTEGRIDAD = "test_integrity_secreto";

    /** JdbcTemplate falso: una fila de wompi_transacciones en memoria. */
    static class JdbcFalso extends JdbcTemplate {
        Map<String, Object> fila;           // null = no existe
        final List<String> sentencias = new ArrayList<>();
        int actualizadas = 1;               // lo que devuelve el UPDATE condicionado de APROBADA

        @Override
        public List<Map<String, Object>> queryForList(String sql, Object... args) {
            return fila == null ? List.of() : List.of(fila);
        }

        @Override
        public int update(String sql, Object... args) {
            sentencias.add(sql);
            if (sql.contains("SET estado = 'APROBADA'")) {
                return actualizadas;
            }
            return 1;
        }
    }

    static class SuscripcionFalsa extends SuscripcionService {
        final List<Object[]> pagos = new ArrayList<>();
        BigDecimal precio = new BigDecimal("104900");

        SuscripcionFalsa() {
            super(null, null, null, null);
        }

        @Override
        public Resumen resumen(Integer clubId) {
            return new Resumen(clubId, "Club", "club", true, 2, "start", "Start", precio, "ACTIVA", "ACTIVA", null, null,
                    5, null, null, null, 0, null, 0);
        }

        @Override
        public Resumen registrarPago(Integer clubId, PagoRequest req, String registradoPor) {
            pagos.add(new Object[] { clubId, req });
            return null;
        }
    }

    static class PaquetesFalsos extends PaquetesService {
        PaquetesService.Vista vista = new PaquetesService.Vista(2, 2, 0, 0, List.of(), List.of());

        PaquetesFalsos() {
            super(null, null);
        }

        @Override
        public Vista vista(int clubId) {
            return vista;
        }
    }

    static Map<String, String> envCompleto() {
        Map<String, String> m = new HashMap<>();
        m.put("WOMPI_PUBLIC_KEY", "pub_test_abc");
        m.put("WOMPI_INTEGRITY_SECRET", SECRETO_INTEGRIDAD);
        m.put("WOMPI_EVENTS_SECRET", SECRETO_EVENTOS);
        m.put("WOMPI_REDIRECT_URL", "https://app.ejemplo.co/plan-y-datos");
        return m;
    }

    static Map<String, Object> evento(String status, long centavos, String moneda, String referencia) {
        Map<String, Object> tx = new HashMap<>();
        tx.put("id", "1234-1610641025-49201");
        tx.put("status", status);
        tx.put("amount_in_cents", centavos);
        tx.put("currency", moneda);
        tx.put("reference", referencia);
        Map<String, Object> data = new HashMap<>();
        data.put("transaction", tx);
        Map<String, Object> firma = new HashMap<>();
        firma.put("properties", List.of("transaction.id", "transaction.status", "transaction.amount_in_cents"));
        Map<String, Object> ev = new HashMap<>();
        ev.put("event", "transaction.updated");
        ev.put("data", data);
        ev.put("timestamp", 1530291411L);
        ev.put("signature", firma);
        firma.put("checksum", WompiService.checksumEsperado(ev, SECRETO_EVENTOS));
        return ev;
    }

    WompiService servicio(JdbcFalso jdbc, SuscripcionFalsa sus, PaquetesFalsos paq, Map<String, String> env) {
        return new WompiService(jdbc, sus, paq, env::get);
    }

    static JdbcFalso conFila(long centavos) {
        JdbcFalso j = new JdbcFalso();
        Map<String, Object> f = new HashMap<>();
        f.put("id", 7);
        f.put("club_id", 2);
        f.put("meses", 3);
        f.put("monto_centavos", centavos);
        j.fila = f;
        return j;
    }

    // ---------------------------------------------------------------- firmas

    @Test
    void firmaDeIntegridadConcatenaReferenciaMontoMonedaYSecreto() {
        assertEquals("608d509773a7a36f1c7798d6ebc05f5a11e73cf4678f5d92be43fc4b1c607955",
                WompiService.firmaIntegridad("sk8-438k4-xmxm392-sn2", 2490000, "COP", "test_integrity_secreto"));
    }

    @Test
    void checksumUsaLasPropiedadesEnOrdenElTimestampYElSecreto() {
        assertEquals("177a142e25beca66d59f857f08b24ca96fb01b5cb39911a05940ea0cb8144a5d",
                WompiService.checksumEsperado(evento("APPROVED", 4490000, "COP", "R"), SECRETO_EVENTOS));
    }

    @Test
    void checksumValidoPorEncabezadoOPorCuerpo() {
        Map<String, Object> ev = evento("APPROVED", 4490000, "COP", "R");
        String bueno = WompiService.checksumEsperado(ev, SECRETO_EVENTOS);
        assertTrue(WompiService.checksumValido(ev, SECRETO_EVENTOS, null));
        assertTrue(WompiService.checksumValido(ev, SECRETO_EVENTOS, bueno.toUpperCase()));
        assertFalse(WompiService.checksumValido(ev, SECRETO_EVENTOS, "00" + bueno.substring(2)));
        assertFalse(WompiService.checksumValido(ev, "otro_secreto", null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void eventoAlteradoNoPasa() {
        Map<String, Object> ev = evento("DECLINED", 4490000, "COP", "R");
        ((Map<String, Object>) ((Map<String, Object>) ev.get("data")).get("transaction")).put("status", "APPROVED");
        assertFalse(WompiService.checksumValido(ev, SECRETO_EVENTOS, null));
    }

    @Test
    void eventoSinFirmaNoPasaNiLanza() {
        assertFalse(WompiService.checksumValido(new HashMap<>(Map.of("event", "transaction.updated")), SECRETO_EVENTOS, "abc"));
    }

    // ---------------------------------------------------------------- puro

    @Test
    void estadosDeWompi() {
        assertEquals("APROBADA", WompiService.estadoDe("APPROVED"));
        assertEquals("RECHAZADA", WompiService.estadoDe("DECLINED"));
        assertEquals("ANULADA", WompiService.estadoDe("VOIDED"));
        assertEquals("ERROR", WompiService.estadoDe("ERROR"));
        assertNull(WompiService.estadoDe("PENDING"));
        assertNull(WompiService.estadoDe(null));
    }

    @Test
    void centavosSonPesosPorMesesPorCien() {
        assertEquals(31470000L, WompiService.centavos(new BigDecimal("104900"), 3));
    }

    @Test
    void costoDePaquetesUsaPrecioAcordadoOElDelPaqueteYSoloLosVigentes() {
        var ofrecidos = List.of(new PaquetesService.Paquete(1, "+25", 25, new BigDecimal("34900")));
        var asignados = List.of(
                new PaquetesService.Asignacion(1, 1, "+25", 2, 25, null, null, null, true),                       // 2 x 34.900
                new PaquetesService.Asignacion(2, 1, "+25", 1, 25, null, null, new BigDecimal("30000"), true),    // 30.000
                new PaquetesService.Asignacion(3, 1, "+25", 5, 25, null, null, null, false));                     // vencido: no cuenta
        assertEquals(new BigDecimal("99800"), WompiService.costoPaquetes(new PaquetesService.Vista(2, 2, 3, 75, ofrecidos, asignados)));
    }

    // ---------------------------------------------------------------- iniciar el pago

    @Test
    void crearCobroFirmaElMontoDelPlanPorMeses() {
        JdbcFalso j = new JdbcFalso();
        WompiService s = servicio(j, new SuscripcionFalsa(), new PaquetesFalsos(), envCompleto());
        WompiService.Cobro c = s.crearCobro(2, 3, "admin");
        assertEquals(31470000L, c.montoCentavos());
        assertTrue(c.sandbox());
        assertTrue(c.url().startsWith("https://checkout.wompi.co/p/?public-key=pub_test_abc&currency=COP&amount-in-cents=31470000"));
        assertTrue(c.url().contains("&signature:integrity=" + WompiService.firmaIntegridad(c.referencia(), 31470000L, "COP", SECRETO_INTEGRIDAD)));
        assertTrue(c.url().contains("redirect-url=https%3A%2F%2Fapp.ejemplo.co%2Fplan-y-datos%3Fpago%3D" + c.referencia()));
        assertTrue(c.referencia().startsWith("CM2-"));
        assertEquals(1, j.sentencias.size());
    }

    @Test
    void crearCobroSumaLosPaquetes() {
        PaquetesFalsos p = new PaquetesFalsos();
        p.vista = new PaquetesService.Vista(2, 2, 1, 25, List.of(),
                List.of(new PaquetesService.Asignacion(1, 1, "+25", 1, 25, null, null, new BigDecimal("34900"), true)));
        WompiService.Cobro c = servicio(new JdbcFalso(), new SuscripcionFalsa(), p, envCompleto()).crearCobro(2, 1, "admin");
        assertEquals(13980000L, c.montoCentavos());
    }

    @Test
    void crearCobroSinLlavesOMesesInvalidosFalla() {
        Map<String, String> sin = envCompleto();
        sin.remove("WOMPI_INTEGRITY_SECRET");
        assertThrows(IllegalStateException.class,
                () -> servicio(new JdbcFalso(), new SuscripcionFalsa(), new PaquetesFalsos(), sin).crearCobro(2, 1, "a"));
        WompiService s = servicio(new JdbcFalso(), new SuscripcionFalsa(), new PaquetesFalsos(), envCompleto());
        assertThrows(RuntimeException.class, () -> s.crearCobro(2, 0, "a"));
        assertThrows(RuntimeException.class, () -> s.crearCobro(2, 13, "a"));
    }

    @Test
    void planSinPrecioNoSeCobraEnLinea() {
        SuscripcionFalsa sus = new SuscripcionFalsa();
        sus.precio = BigDecimal.ZERO;
        WompiService s = servicio(new JdbcFalso(), sus, new PaquetesFalsos(), envCompleto());
        assertFalse(s.estado(2).habilitado());
        assertThrows(RuntimeException.class, () -> s.crearCobro(2, 1, "a"));
    }

    // ---------------------------------------------------------------- webhook

    @Test
    void pagoAprobadoRegistraLaSuscripcionUnaSolaVez() {
        JdbcFalso j = conFila(31470000L);
        SuscripcionFalsa sus = new SuscripcionFalsa();
        WompiService s = servicio(j, sus, new PaquetesFalsos(), envCompleto());
        Map<String, Object> ev = evento("APPROVED", 31470000L, "COP", "CM2-abc");

        assertEquals("APROBADA", s.procesarEvento(ev, null));
        assertEquals(1, sus.pagos.size());
        SuscripcionService.PagoRequest req = (SuscripcionService.PagoRequest) sus.pagos.get(0)[1];
        assertEquals(2, sus.pagos.get(0)[0]);
        assertEquals(3, req.meses());
        assertEquals(new BigDecimal("314700"), req.monto());
        assertEquals("WOMPI", req.metodo());
        assertEquals("1234-1610641025-49201", req.referencia());

        j.actualizadas = 0; // Wompi reintenta el mismo evento: la fila ya esta APROBADA
        assertEquals("YA_PROCESADA", s.procesarEvento(ev, null));
        assertEquals(1, sus.pagos.size());
    }

    @Test
    void montoOMonedaDistintosNoRegistranNada() {
        SuscripcionFalsa sus = new SuscripcionFalsa();
        WompiService s = servicio(conFila(31470000L), sus, new PaquetesFalsos(), envCompleto());
        assertEquals("MONTO_NO_COINCIDE", s.procesarEvento(evento("APPROVED", 100L, "COP", "CM2-abc"), null));
        assertEquals("MONTO_NO_COINCIDE", s.procesarEvento(evento("APPROVED", 31470000L, "USD", "CM2-abc"), null));
        assertTrue(sus.pagos.isEmpty());
    }

    @Test
    void rechazadoOAnuladoSoloCambiaElEstado() {
        SuscripcionFalsa sus = new SuscripcionFalsa();
        WompiService s = servicio(conFila(31470000L), sus, new PaquetesFalsos(), envCompleto());
        assertEquals("RECHAZADA", s.procesarEvento(evento("DECLINED", 31470000L, "COP", "CM2-abc"), null));
        assertEquals("ANULADA", s.procesarEvento(evento("VOIDED", 31470000L, "COP", "CM2-abc"), null));
        assertEquals("EN_PROCESO", s.procesarEvento(evento("PENDING", 31470000L, "COP", "CM2-abc"), null));
        assertTrue(sus.pagos.isEmpty());
    }

    @Test
    void referenciaDesconocidaYEventosAjenosSeIgnoran() {
        SuscripcionFalsa sus = new SuscripcionFalsa();
        JdbcFalso sinFila = new JdbcFalso();
        WompiService s = servicio(sinFila, sus, new PaquetesFalsos(), envCompleto());
        assertEquals("SIN_REFERENCIA", s.procesarEvento(evento("APPROVED", 1L, "COP", "OTRA"), null));
        Map<String, Object> otro = evento("APPROVED", 1L, "COP", "OTRA");
        otro.put("event", "nequi_token.updated");
        assertEquals("IGNORADO", s.procesarEvento(otro, null));
        assertTrue(sus.pagos.isEmpty());
    }

    @Test
    void checksumInvalidoSeRechazaSinTocarNada() {
        SuscripcionFalsa sus = new SuscripcionFalsa();
        JdbcFalso j = conFila(31470000L);
        WompiService s = servicio(j, sus, new PaquetesFalsos(), envCompleto());
        Map<String, Object> ev = evento("APPROVED", 31470000L, "COP", "CM2-abc");
        assertThrows(SecurityException.class, () -> s.procesarEvento(ev, "0000"));
        assertTrue(sus.pagos.isEmpty());
        assertTrue(j.sentencias.isEmpty());
    }

    @Test
    void sinSecretoDeEventosNoProcesaNada() {
        Map<String, String> sin = envCompleto();
        sin.remove("WOMPI_EVENTS_SECRET");
        WompiService s = servicio(conFila(1L), new SuscripcionFalsa(), new PaquetesFalsos(), sin);
        assertThrows(IllegalStateException.class, () -> s.procesarEvento(evento("APPROVED", 1L, "COP", "CM2-abc"), null));
    }
}
