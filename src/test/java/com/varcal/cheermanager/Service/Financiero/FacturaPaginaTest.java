package com.varcal.cheermanager.Service.Financiero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.varcal.cheermanager.DTO.Comunes.PaginaDTO;
import com.varcal.cheermanager.DTO.Financiero.FacturaResponseDTO;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
import com.varcal.cheermanager.repository.Financiero.PagoRepository;

/** Reglas de entrada del listado paginado: valores por defecto, limites y validaciones. */
class FacturaPaginaTest {

    private final FacturaRepository facturas = mock(FacturaRepository.class);
    private final PagoRepository pagos = mock(PagoRepository.class);
    private final FacturaService service = new FacturaService(facturas, null, pagos, null);

    @Test
    void sinFiltrosUsaValoresPorDefectoYNoPideMasDe100() {
        Page<Factura> vacia = new PageImpl<>(List.of());
        when(facturas.buscarPagina(anyString(), anyString(), anyString(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(vacia);

        PaginaDTO<FacturaResponseDTO> r = service.listarPagina(null, null, null, null, null, -5, 5000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(facturas).buscarPagina(eq(""), eq("TODOS"), eq("TODOS"), any(LocalDate.class), any(LocalDate.class),
                any(LocalDate.class), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());   // pagina negativa -> 0
        assertEquals(100, pageable.getValue().getPageSize());   // tamano limitado a 100
        assertEquals(0, r.totalElementos());
        // Lista vacia: no se consulta el total pagado de nada.
        verify(pagos, never()).pagadoPorFacturas(any());
    }

    @Test
    void estadoDesconocidoEsError() {
        assertThrows(IllegalArgumentException.class,
                () -> service.listarPagina(null, "INVENTADO", null, null, null, 0, 20));
    }

    @Test
    void rangoDeFechasInvertidoEsError() {
        assertThrows(IllegalArgumentException.class, () -> service.listarPagina(
                null, null, null, LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1), 0, 20));
    }

    @Test
    void estadoYBusquedaSeNormalizan() {
        when(facturas.buscarPagina(anyString(), anyString(), anyString(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listarPagina("  maria ", "en_mora", "uniforme", null, null, 2, 20);

        verify(facturas).buscarPagina(eq("maria"), eq("EN_MORA"), eq("UNIFORME"), any(), any(), any(), any(Pageable.class));
    }
}
