package com.varcal.cheermanager.Service.Financiero;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Service.Financiero.FacturacionConfigService.ConfigDTO;

class FacturacionConfigServiceTest {

    private static ConfigDTO dto(String nit, String dv, String iva, BigDecimal tarifa, String email, Integer rango) {
        return new ConfigDTO(false, "FACTUS", "PRUEBAS", "PAGO", "NATURAL", nit, dv, "Leandro Vargas", null, true, iva,
                tarifa, "18001", "Calle 1", "300", email, rango, "RA");
    }

    @Test
    void dvDelNitConocidoDeLaDian() {
        assertEquals(4, FacturacionConfigService.calcularDv("800197268")); // NIT de la DIAN
        assertEquals(8, FacturacionConfigService.calcularDv("890903938")); // Bancolombia
        assertEquals(4, FacturacionConfigService.calcularDv("900373913"));
    }

    @Test
    void dvIgnoraPuntosYGuiones() {
        assertEquals(FacturacionConfigService.calcularDv("900373913"), FacturacionConfigService.calcularDv("900.373.913"));
    }

    @Test
    void nitInvalidoLanza() {
        assertThrows(IllegalArgumentException.class, () -> FacturacionConfigService.calcularDv(""));
        assertThrows(IllegalArgumentException.class, () -> FacturacionConfigService.calcularDv("1234567890123456"));
    }

    @Test
    void dvQueNoCorrespondeSeReporta() {
        List<String> e = FacturacionConfigService.errores(dto("900373913", "1", "EXCLUIDO", BigDecimal.ZERO, "a@b.co", 1));
        assertEquals(1, e.size());
        assertTrue(e.get(0).contains("deberia ser 4"));
    }

    @Test
    void dvCorrectoNoDaErrores() {
        assertTrue(FacturacionConfigService.errores(dto("900373913", "4", "EXCLUIDO", BigDecimal.ZERO, "a@b.co", 1)).isEmpty());
    }

    @Test
    void erroresDeForma() {
        ConfigDTO malo = new ConfigDTO(false, "OTRO", "X", "Y", "Z", null, null, null, null, false, "Q",
                new BigDecimal("150"), null, null, null, "no-es-correo", null, null);
        assertEquals(7, FacturacionConfigService.errores(malo).size());
    }

    @Test
    void faltantesParaActivar() {
        ConfigDTO vacio = new ConfigDTO(false, "FACTUS", "PRUEBAS", "PAGO", null, null, null, null, null, false,
                "EXCLUIDO", BigDecimal.ZERO, null, null, null, null, null, null);
        assertEquals(8, FacturacionConfigService.faltantes(vacio).size());
        assertTrue(FacturacionConfigService.faltantes(dto("900373913", "4", "EXCLUIDO", BigDecimal.ZERO, "a@b.co", 1)).isEmpty());
    }

    @Test
    void gravadoExigeTarifa() {
        List<String> f = FacturacionConfigService.faltantes(dto("900373913", "4", "GRAVADO", BigDecimal.ZERO, "a@b.co", 1));
        assertEquals(List.of("tarifa de IVA"), f);
        assertTrue(FacturacionConfigService.faltantes(dto("900373913", "4", "GRAVADO", new BigDecimal("19"), "a@b.co", 1)).isEmpty());
    }
}
