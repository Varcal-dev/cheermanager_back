package com.varcal.cheermanager.Service.Financiero;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.varcal.cheermanager.DTO.Comunes.PaginaDTO;
import com.varcal.cheermanager.DTO.Financiero.PagoResponseDTO;
import com.varcal.cheermanager.Models.Financiero.Pago;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
import com.varcal.cheermanager.repository.Financiero.PagoRepository;

/** PERF-01: reglas de entrada del listado paginado de pagos y de las tarjetas (resumen). Sin base de datos. */
class PagoPaginaTest {

    private final PagoRepository pagos = mock(PagoRepository.class);
    private final FacturaRepository facturas = mock(FacturaRepository.class);
    private final PagoService service = new PagoService();

    PagoPaginaTest() {
        ReflectionTestUtils.setField(service, "pagoRepository", pagos);
        ReflectionTestUtils.setField(service, "facturaRepository", facturas);
    }

    @Test
    void sinFiltrosUsaValoresPorDefectoYNoPideMasDe100() {
        Page<Pago> vacia = new PageImpl<>(List.of());
        when(pagos.buscarPagina(anyString(), anyString(), any(), any(), any(Pageable.class))).thenReturn(vacia);

        PaginaDTO<PagoResponseDTO> r = service.listarPagina(null, null, null, null, -5, 5000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(pagos).buscarPagina(eq(""), eq("TODOS"), any(LocalDate.class), any(LocalDate.class), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());   // pagina negativa -> 0
        assertEquals(100, pageable.getValue().getPageSize());   // tamano limitado a 100
        assertEquals(0, r.totalElementos());
        // Lista vacia: no se consulta el total pagado de nada.
        verify(pagos, never()).pagadoPorFacturas(any());
    }

    @Test
    void normalizaElEstadoYRecortaLaBusqueda() {
        when(pagos.buscarPagina(anyString(), anyString(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        service.listarPagina("  juan  ", "  anulado ", null, null, 2, 0);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(pagos).buscarPagina(eq("juan"), eq("ANULADO"), any(LocalDate.class), any(LocalDate.class), pageable.capture());
        assertEquals(2, pageable.getValue().getPageNumber());
        assertEquals(1, pageable.getValue().getPageSize());     // tamano minimo 1
    }

    @Test
    void estadoDesconocidoEsError() {
        assertThrows(IllegalArgumentException.class, () -> service.listarPagina(null, "INVENTADO", null, null, 0, 20));
        // "CON_SALDO" es un estado de FACTURAS, no de pagos.
        assertThrows(IllegalArgumentException.class, () -> service.listarPagina(null, "CON_SALDO", null, null, 0, 20));
    }

    @Test
    void rangoDeFechasInvertidoEsError() {
        assertThrows(IllegalArgumentException.class,
                () -> service.listarPagina(null, null, LocalDate.of(2026, 5, 2), LocalDate.of(2026, 5, 1), 0, 20));
    }

    @Test
    void resumenEsRecaudadoYSaldoPendienteDeLasFacturasConSaldo() {
        when(pagos.sumarTotalRecaudado()).thenReturn(new BigDecimal("500.00"));
        when(facturas.sumarTotalFacturasConSaldo()).thenReturn(new BigDecimal("300.00"));
        when(pagos.sumarPagadoEnFacturasConSaldo()).thenReturn(new BigDecimal("120.00"));

        Map<String, Object> r = service.resumen();

        assertEquals(new BigDecimal("500.00"), r.get("totalRecaudado"));
        assertEquals(new BigDecimal("180.00"), r.get("totalPendiente"));
    }

    @Test
    void resumenNuncaDevuelveUnPendienteNegativo() {
        when(pagos.sumarTotalRecaudado()).thenReturn(BigDecimal.ZERO);
        when(facturas.sumarTotalFacturasConSaldo()).thenReturn(new BigDecimal("100.00"));
        when(pagos.sumarPagadoEnFacturasConSaldo()).thenReturn(new BigDecimal("150.00"));   // dato incoherente

        assertEquals(0, ((BigDecimal) service.resumen().get("totalPendiente")).signum());
    }
}
