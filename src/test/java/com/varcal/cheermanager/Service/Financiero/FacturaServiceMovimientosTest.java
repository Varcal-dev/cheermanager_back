package com.varcal.cheermanager.Service.Financiero;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.DTO.Financiero.FacturaManualDTO;
import com.varcal.cheermanager.DTO.Financiero.FacturaResponseDTO;
import com.varcal.cheermanager.Models.Financiero.EstadoPago;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.MundoFinanciero;

/** Cobros manuales, numeracion y anulacion de facturas. Sin base de datos. */
class FacturaServiceMovimientosTest {

    static FacturaManualDTO manual(Integer personaId, String concepto, String total) {
        FacturaManualDTO d = new FacturaManualDTO();
        d.setPersonaId(personaId);
        d.setConcepto(concepto);
        d.setDescripcion("Cobro de prueba");
        d.setTotal(total == null ? null : new BigDecimal(total));
        return d;
    }

    @Test
    void crearManualGeneraNumeroConsecutivoVencimientoYTotalFinal() {
        MundoFinanciero w = new MundoFinanciero();
        w.persona(1, "Ana");
        FacturaResponseDTO a = w.facturaService.crearManual(manual(1, "uniforme", "120000"));
        FacturaResponseDTO b = w.facturaService.crearManual(manual(1, "OTRO", "5000.555"));
        String prefijo = "FAC-" + Year.now().getValue() + "-";
        assertEquals(prefijo + "0001", a.getNumeroFactura());
        assertEquals(prefijo + "0002", b.getNumeroFactura());
        assertEquals("UNIFORME", a.getConcepto());
        assertEquals(com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().plusDays(FacturaService.DIAS_PARA_PAGAR), a.getFechaVencimiento());
        assertTrue(Fakes.igual("5000.56", b.getTotal()));
        assertEquals("PENDIENTE", a.getEstadoCuenta());
    }

    @Test
    void crearManualValidaPersonaConceptoDescripcionTotalEventoYFechas() {
        MundoFinanciero w = new MundoFinanciero();
        w.persona(1, "Ana");
        assertThrows(RuntimeException.class, () -> w.facturaService.crearManual(manual(null, "OTRO", "10")));
        assertThrows(RuntimeException.class, () -> w.facturaService.crearManual(manual(99, "OTRO", "10")));
        assertThrows(RuntimeException.class, () -> w.facturaService.crearManual(manual(1, "MENSUALIDAD", "10")));
        assertThrows(RuntimeException.class, () -> w.facturaService.crearManual(manual(1, null, "10")));
        assertThrows(RuntimeException.class, () -> w.facturaService.crearManual(manual(1, "OTRO", null)));
        assertThrows(RuntimeException.class, () -> w.facturaService.crearManual(manual(1, "OTRO", "0")));
        assertThrows(RuntimeException.class, () -> w.facturaService.crearManual(manual(1, "OTRO", "-1")));
        FacturaManualDTO sinDesc = manual(1, "OTRO", "10");
        sinDesc.setDescripcion("  ");
        assertThrows(RuntimeException.class, () -> w.facturaService.crearManual(sinDesc));
        FacturaManualDTO eventoMalo = manual(1, "EVENTO", "10");
        eventoMalo.setEventoId(777);
        assertThrows(RuntimeException.class, () -> w.facturaService.crearManual(eventoMalo));
        FacturaManualDTO vencidaAntes = manual(1, "OTRO", "10");
        vencidaAntes.setFechaEmision(com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy());
        vencidaAntes.setFechaVencimiento(com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().minusDays(1));
        assertThrows(RuntimeException.class, () -> w.facturaService.crearManual(vencidaAntes));
        assertEquals(0, w.facturas.size());

        w.eventosExistentes.add(777);
        assertEquals(777, (int) w.facturaService.crearManual(eventoMalo).getEventoId());
    }

    @Test
    void anularFacturaSinPagosDejaMotivoYNoSeBorra() {
        MundoFinanciero w = new MundoFinanciero();
        Factura f = w.factura(1, "50000", com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().plusDays(3));
        FacturaResponseDTO r = w.facturaService.anular(f.getId(), "Cobro duplicado");
        assertTrue(r.getAnulada());
        assertEquals("ANULADA", r.getEstadoCuenta());
        assertEquals("Cobro duplicado", r.getMotivoAnulacion());
        assertTrue(Fakes.igual("0", r.getSaldoPendiente()));
        assertEquals(1, w.facturas.size());
    }

    @Test
    void anularFacturaConPagoPagadoSeBloqueaYNoCambiaNada() {
        MundoFinanciero w = new MundoFinanciero();
        Factura f = w.factura(1, "50000", com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().plusDays(3));
        w.pagoDirecto(f, "20000", EstadoPago.Pagado, com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy());
        w.pagoDirecto(f, "10000", EstadoPago.Pendiente, com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy());
        assertThrows(RuntimeException.class, () -> w.facturaService.anular(f.getId(), "Quiero anularla"));
        assertFalse(f.getAnulada());
        assertEquals(EstadoPago.Pendiente, w.pagos.get(1).getEstado());
    }

    @Test
    void anularPrimeroElPagoPermiteLuegoAnularLaFactura() {
        MundoFinanciero w = new MundoFinanciero();
        Factura f = w.factura(1, "50000", com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().plusDays(3));
        var p = w.pagoDirecto(f, "20000", EstadoPago.Pagado, com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy());
        w.pagoService.anular(p.getId(), "Pago mal digitado");
        FacturaResponseDTO r = w.facturaService.anular(f.getId(), "Factura mal generada");
        assertEquals("ANULADA", r.getEstadoCuenta());
    }

    @Test
    void anularFacturaAnulaSusPagosPendientesYVencidos() {
        MundoFinanciero w = new MundoFinanciero();
        Factura f = w.factura(1, "50000", com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().plusDays(3));
        w.pagoDirecto(f, "10000", EstadoPago.Pendiente, com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy());
        w.pagoDirecto(f, "10000", EstadoPago.Vencido, com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy());
        w.facturaService.anular(f.getId(), "Se cancela el servicio");
        assertTrue(w.pagos.stream().allMatch(p -> p.getEstado() == EstadoPago.Anulado));
        assertTrue(w.pagos.get(0).getMotivoAnulacion().contains("Se cancela el servicio"));
    }

    @Test
    void anularExigeMotivoNoRepiteYFacturaInexistente() {
        MundoFinanciero w = new MundoFinanciero();
        Factura f = w.factura(1, "50000", com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().plusDays(3));
        assertThrows(RuntimeException.class, () -> w.facturaService.anular(f.getId(), ""));
        assertThrows(RuntimeException.class, () -> w.facturaService.anular(f.getId(), "abc"));
        assertThrows(RuntimeException.class, () -> w.facturaService.anular(999, "Motivo valido"));
        w.facturaService.anular(f.getId(), "Motivo valido");
        assertThrows(RuntimeException.class, () -> w.facturaService.anular(f.getId(), "Otra vez"));
    }

    @Test
    void estadoDeCuentaSigueElFlujoPendienteAbonoMoraPagada() {
        MundoFinanciero w = new MundoFinanciero();
        Factura vigente = w.factura(1, "100000", com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().plusDays(2));
        Factura vencida = w.factura(1, "100000", com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().minusDays(4));
        assertEquals("PENDIENTE", w.facturaService.toResponseDTO(vigente).getEstadoCuenta());
        w.pagoService.registrarPago(PagoServiceTest.dto(vigente.getId(), "30000", "Pagado", com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy()));
        assertEquals("ABONO_PARCIAL", w.facturaService.toResponseDTO(vigente).getEstadoCuenta());
        FacturaResponseDTO mora = w.facturaService.toResponseDTO(vencida);
        assertEquals("EN_MORA", mora.getEstadoCuenta());
        assertEquals(4L, (long) mora.getDiasMora());
        w.pagoService.registrarPago(PagoServiceTest.dto(vencida.getId(), "100000", "Pagado", com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy()));
        assertEquals("PAGADA", w.facturaService.toResponseDTO(vencida).getEstadoCuenta());
    }

    @Test
    void numeracionDeVentasUsaSuPropiaSerie() {
        MundoFinanciero w = new MundoFinanciero();
        assertTrue(w.facturaService.generarNumeroFacturaVenta().startsWith("FAC-VTA-" + Year.now().getValue() + "-"));
        assertEquals("FAC-2030-0001", w.facturaService.generarNumeroFactura("FAC-2030-"));
    }
}
