package com.varcal.cheermanager.Service.Financiero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.DTO.Financiero.FacturaResponseDTO;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.repository.Financiero.PagoRepository;

class FacturaEstadoCuentaTest {

    private FacturaResponseDTO dto(Factura f, String pagado) {
        PagoRepository pagos = mock(PagoRepository.class);
        when(pagos.sumarMontoPagadoPorFactura(any())).thenReturn(new BigDecimal(pagado));
        return new FacturaService(null, null, pagos, null).toResponseDTO(f);
    }

    private Factura factura(LocalDate vence, boolean anulada) {
        Factura f = new Factura();
        f.setId(1);
        f.setNumeroFactura("FAC-2026-0001");
        f.setFechaEmision(vence.minusDays(10));
        f.setFechaVencimiento(vence);
        f.setTotal(new BigDecimal("100000.00"));
        f.setAnulada(anulada);
        return f;
    }

    @Test
    void pendienteSiNoHaVencido() {
        FacturaResponseDTO r = dto(factura(LocalDate.now().plusDays(3), false), "0");
        assertEquals("PENDIENTE", r.getEstadoCuenta());
        assertNull(r.getDiasMora());
    }

    @Test
    void abonoParcialSiNoHaVencido() {
        assertEquals("ABONO_PARCIAL", dto(factura(LocalDate.now().plusDays(3), false), "40000").getEstadoCuenta());
    }

    @Test
    void enMoraDespuesDelVencimientoConSaldo() {
        FacturaResponseDTO r = dto(factura(LocalDate.now().minusDays(4), false), "40000");
        assertEquals("EN_MORA", r.getEstadoCuenta());
        assertEquals(4L, r.getDiasMora());
        assertEquals(new BigDecimal("60000.00"), r.getSaldoPendiente());
    }

    @Test
    void pagadaNuncaEstaEnMora() {
        assertEquals("PAGADA", dto(factura(LocalDate.now().minusDays(30), false), "100000").getEstadoCuenta());
    }

    @Test
    void anuladaNoTieneSaldo() {
        FacturaResponseDTO r = dto(factura(LocalDate.now().minusDays(30), true), "0");
        assertEquals("ANULADA", r.getEstadoCuenta());
        assertEquals(BigDecimal.ZERO, r.getSaldoPendiente());
    }
}
