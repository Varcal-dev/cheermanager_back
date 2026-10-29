package com.varcal.cheermanager.Service.Financiero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.DTO.Financiero.FacturaResponseDTO;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.repository.Financiero.PagoRepository;

/** El listado por lote debe dar el MISMO resultado que el individual, con UNA sola consulta de pagos. */
class FacturaListadoPorLoteTest {

    private Factura factura(int id, LocalDate vence) {
        Factura f = new Factura();
        f.setId(id);
        f.setNumeroFactura("FAC-2026-000" + id);
        f.setFechaEmision(vence.minusDays(10));
        f.setFechaVencimiento(vence);
        f.setTotal(new BigDecimal("100000.00"));
        f.setAnulada(false);
        return f;
    }

    @Test
    void unaSolaConsultaDePagosParaTodasLasFacturas() {
        PagoRepository pagos = mock(PagoRepository.class);
        // Factura 1 abonó 40.000; la 2 y la 3 no tienen pagos (no aparecen en el mapa).
        when(pagos.pagadoPorFacturas(any())).thenReturn(Map.of(1, new BigDecimal("40000")));
        FacturaService service = new FacturaService(null, null, pagos, null);

        List<FacturaResponseDTO> r = service.toResponseDTOs(List.of(
                factura(1, LocalDate.now().minusDays(4)),
                factura(2, LocalDate.now().plusDays(3)),
                factura(3, LocalDate.now().minusDays(1))));

        assertEquals(3, r.size());
        assertEquals("EN_MORA", r.get(0).getEstadoCuenta());
        assertEquals(new BigDecimal("60000.00"), r.get(0).getSaldoPendiente());
        assertEquals("PENDIENTE", r.get(1).getEstadoCuenta());
        assertEquals(new BigDecimal("100000.00"), r.get(1).getSaldoPendiente());
        assertEquals("EN_MORA", r.get(2).getEstadoCuenta());

        verify(pagos, times(1)).pagadoPorFacturas(any());
        verify(pagos, never()).sumarMontoPagadoPorFactura(any());
    }

    @Test
    void listaVaciaNoConsultaNada() {
        PagoRepository pagos = mock(PagoRepository.class);
        assertEquals(0, new FacturaService(null, null, pagos, null).toResponseDTOs(List.of()).size());
        verify(pagos, never()).pagadoPorFacturas(any());
    }
}
