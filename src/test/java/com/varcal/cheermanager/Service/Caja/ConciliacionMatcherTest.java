package com.varcal.cheermanager.Service.Caja;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Service.Caja.ConciliacionMatcher.Candidato;
import com.varcal.cheermanager.Service.Caja.ConciliacionMatcher.Linea;
import com.varcal.cheermanager.Service.Caja.ConciliacionMatcher.Resultado;

class ConciliacionMatcherTest {

    private static LocalDate d(int dia) {
        return LocalDate.of(2026, 10, dia);
    }

    private static BigDecimal b(String s) {
        return new BigDecimal(s);
    }

    @Test
    void cruzaPorReferenciaAunqueLaFechaDifiera() {
        Resultado r = ConciliacionMatcher.cruzar(
                List.of(new Linea(2, d(20), b("100000"), "TRF-00123")),
                List.of(new Candidato(7, d(15), b("100000.00"), "trf 00123")), 3);
        assertEquals(1, r.coincidencias().size());
        assertEquals(ConciliacionMatcher.REFERENCIA, r.coincidencias().get(0).criterio());
        assertEquals(7, r.coincidencias().get(0).pagoId());
    }

    @Test
    void referenciaConOtroValorNoCruza() {
        Resultado r = ConciliacionMatcher.cruzar(
                List.of(new Linea(2, d(20), b("100000"), "TRF-00123")),
                List.of(new Candidato(7, d(20), b("90000"), "TRF-00123")), 3);
        assertTrue(r.coincidencias().isEmpty());
        assertEquals(List.of(2), r.lineasSinPago());
    }

    @Test
    void cruzaPorMontoYFechaDentroDeLaVentana() {
        Resultado r = ConciliacionMatcher.cruzar(
                List.of(new Linea(2, d(20), b("50000"), "")),
                List.of(new Candidato(1, d(18), b("50000"), null)), 3);
        assertEquals(ConciliacionMatcher.MONTO_FECHA, r.coincidencias().get(0).criterio());
        assertEquals(2, r.coincidencias().get(0).diasDiferencia());
        // fuera de la ventana
        Resultado f = ConciliacionMatcher.cruzar(
                List.of(new Linea(2, d(20), b("50000"), "")),
                List.of(new Candidato(1, d(10), b("50000"), null)), 3);
        assertTrue(f.coincidencias().isEmpty());
    }

    @Test
    void dosPagosPosiblesSeMarcanAmbiguos() {
        Resultado r = ConciliacionMatcher.cruzar(
                List.of(new Linea(2, d(20), b("50000"), "")),
                List.of(new Candidato(1, d(19), b("50000"), null), new Candidato(2, d(21), b("50000"), null)), 3);
        assertEquals(1, r.coincidencias().size());
        assertEquals(ConciliacionMatcher.AMBIGUA, r.coincidencias().get(0).criterio());
        assertEquals(1, r.coincidencias().get(0).pagoId()); // empate de distancia: el de menor id
    }

    @Test
    void dosLineasPorUnSoloPagoSonAmbiguas() {
        Resultado r = ConciliacionMatcher.cruzar(
                List.of(new Linea(2, d(20), b("50000"), ""), new Linea(3, d(20), b("50000"), "")),
                List.of(new Candidato(1, d(20), b("50000"), null)), 3);
        assertEquals(1, r.coincidencias().size());
        assertEquals(ConciliacionMatcher.AMBIGUA, r.coincidencias().get(0).criterio());
        assertEquals(1, r.lineasSinPago().size());
    }

    @Test
    void cadaPagoSeUsaUnaSolaVez() {
        Resultado r = ConciliacionMatcher.cruzar(
                List.of(new Linea(2, d(20), b("50000"), "REF-AAAA"), new Linea(3, d(20), b("50000"), "")),
                List.of(new Candidato(1, d(20), b("50000"), "REF-AAAA")), 3);
        assertEquals(1, r.coincidencias().size());
        assertEquals(ConciliacionMatcher.REFERENCIA, r.coincidencias().get(0).criterio());
        assertEquals(List.of(3), r.lineasSinPago());
    }

    @Test
    void pagosSinLineaSoloDentroDelPeriodoDelExtracto() {
        Resultado r = ConciliacionMatcher.cruzar(
                List.of(new Linea(2, d(10), b("1000"), ""), new Linea(3, d(20), b("2000"), "")),
                List.of(new Candidato(1, d(15), b("777"), null), // dentro del periodo, sin linea
                        new Candidato(2, d(28), b("888"), null), // fuera del periodo: no se reporta
                        new Candidato(3, d(10), b("1000"), null)), 3);
        assertEquals(List.of(1), r.pagosSinLinea());
    }

    @Test
    void referenciaCortaSeIgnora() {
        assertEquals("", ConciliacionMatcher.clave("12"));
        assertEquals("TRF123", ConciliacionMatcher.clave("trf-123"));
    }
}
