package com.varcal.cheermanager.Service.Financiero;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.DTO.Financiero.PagoDTO;
import com.varcal.cheermanager.DTO.Financiero.PagoResponseDTO;
import com.varcal.cheermanager.Models.Financiero.EstadoPago;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.Models.Financiero.Pago;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.MundoFinanciero;

/** Reglas de dinero de PagoService: abonos, sobrepago, anulaciones y caja cerrada. Sin base de datos. */
class PagoServiceTest {

    static PagoDTO dto(Integer facturaId, String monto, String estado, LocalDate fecha) {
        PagoDTO d = new PagoDTO();
        d.setFacturaId(facturaId);
        d.setTipoPagoId(1);
        d.setMetodoPagoId(1);
        d.setMonto(monto == null ? null : new BigDecimal(monto));
        d.setEstado(estado);
        d.setFecha(fecha);
        return d;
    }

    @Test
    void abonoParcialDejaSaldoYLuegoElPagoCompletoLoCierra() {
        MundoFinanciero w = new MundoFinanciero();
        Factura f = w.factura(1, "100000", LocalDate.now().plusDays(5));
        PagoResponseDTO r1 = w.pagoService.registrarPago(dto(f.getId(), "40000", "Pagado", LocalDate.now()));
        assertTrue(Fakes.igual("60000", r1.getSaldoPendienteFactura()));
        PagoResponseDTO r2 = w.pagoService.registrarPago(dto(f.getId(), "60000", "Pagado", LocalDate.now()));
        assertTrue(Fakes.igual("0", r2.getSaldoPendienteFactura()));
        assertTrue(Fakes.igual("0", w.pagoService.calcularSaldoPendiente(f.getId())));
        assertEquals("PAGADA", w.facturaService.toResponseDTO(f).getEstadoCuenta());
    }

    @Test
    void sobrepagoSeRechazaYNoGuardaNada() {
        MundoFinanciero w = new MundoFinanciero();
        Factura f = w.factura(1, "100000", LocalDate.now().plusDays(5));
        w.pagoService.registrarPago(dto(f.getId(), "70000", "Pagado", LocalDate.now()));
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> w.pagoService.registrarPago(dto(f.getId(), "30000.01", "Pagado", LocalDate.now())));
        assertTrue(e.getMessage().contains("excede"));
        assertEquals(1, w.pagos.size());
    }

    @Test
    void unPagoPendienteNoDescuentaSaldoNiCuentaParaSobrepago() {
        MundoFinanciero w = new MundoFinanciero();
        Factura f = w.factura(1, "100000", LocalDate.now().plusDays(5));
        w.pagoService.registrarPago(dto(f.getId(), "100000", "Pendiente", LocalDate.now()));
        assertTrue(Fakes.igual("100000", w.pagoService.calcularSaldoPendiente(f.getId())));
        // y todavia se puede pagar completo
        w.pagoService.registrarPago(dto(f.getId(), "100000", "Pagado", LocalDate.now()));
        assertTrue(Fakes.igual("0", w.pagoService.calcularSaldoPendiente(f.getId())));
    }

    @Test
    void marcarPendienteComoPagadoRevisaElSobrepago() {
        MundoFinanciero w = new MundoFinanciero();
        Factura f = w.factura(1, "100000", LocalDate.now().plusDays(5));
        PagoResponseDTO pendiente = w.pagoService.registrarPago(dto(f.getId(), "80000", "Pendiente", LocalDate.now()));
        w.pagoService.registrarPago(dto(f.getId(), "50000", "Pagado", LocalDate.now()));
        // 50000 + 80000 > 100000: no se puede confirmar el pendiente
        assertThrows(RuntimeException.class, () -> w.pagoService.actualizarEstado(pendiente.getId(), "Pagado"));
        assertEquals(EstadoPago.Pendiente, w.pagos.get(0).getEstado());
    }

    @Test
    void validacionesBasicasDelPago() {
        MundoFinanciero w = new MundoFinanciero();
        Factura f = w.factura(1, "100000", LocalDate.now().plusDays(5));
        assertThrows(RuntimeException.class, () -> w.pagoService.registrarPago(dto(f.getId(), null, "Pagado", LocalDate.now())));
        assertThrows(RuntimeException.class, () -> w.pagoService.registrarPago(dto(f.getId(), "0", "Pagado", LocalDate.now())));
        assertThrows(RuntimeException.class, () -> w.pagoService.registrarPago(dto(f.getId(), "-10", "Pagado", LocalDate.now())));
        assertThrows(RuntimeException.class, () -> w.pagoService.registrarPago(dto(f.getId(), "10", "Inventado", LocalDate.now())));
        assertThrows(RuntimeException.class, () -> w.pagoService.registrarPago(dto(f.getId(), "10", null, LocalDate.now())));
        assertThrows(RuntimeException.class, () -> w.pagoService.registrarPago(dto(f.getId(), "10", "Pagado", null)));
        assertThrows(RuntimeException.class, () -> w.pagoService.registrarPago(dto(999, "10", "Pagado", LocalDate.now())));
        // "Anulado" no se registra directo: se anula con motivo
        assertThrows(RuntimeException.class, () -> w.pagoService.registrarPago(dto(f.getId(), "10", "Anulado", LocalDate.now())));
        assertEquals(0, w.pagos.size());
    }

    @Test
    void unaFacturaAnuladaNoAdmitePagos() {
        MundoFinanciero w = new MundoFinanciero();
        Factura f = w.factura(1, "100000", LocalDate.now().plusDays(5));
        f.setAnulada(true);
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> w.pagoService.registrarPago(dto(f.getId(), "10000", "Pagado", LocalDate.now())));
        assertTrue(e.getMessage().contains("anulada"));
        assertEquals(0, w.pagos.size());
    }

    @Test
    void anularUnPagoLiberaElSaldoYQuedaConMotivo() {
        MundoFinanciero w = new MundoFinanciero();
        Factura f = w.factura(1, "100000", LocalDate.now().plusDays(5));
        PagoResponseDTO p = w.pagoService.registrarPago(dto(f.getId(), "100000", "Pagado", LocalDate.now()));
        PagoResponseDTO anulado = w.pagoService.anular(p.getId(), "Error de digitacion");
        assertEquals("Anulado", anulado.getEstado());
        assertEquals("Error de digitacion", anulado.getMotivoAnulacion());
        assertNotNull(anulado.getFechaAnulacion());
        assertTrue(Fakes.igual("100000", w.pagoService.calcularSaldoPendiente(f.getId())));
        // el pago sigue existiendo (no se borra)
        assertEquals(1, w.pagos.size());
    }

    @Test
    void anularExigeMotivoYNoSePuedeAnularDosVecesNiReactivar() {
        MundoFinanciero w = new MundoFinanciero();
        Factura f = w.factura(1, "100000", LocalDate.now().plusDays(5));
        PagoResponseDTO p = w.pagoService.registrarPago(dto(f.getId(), "50000", "Pagado", LocalDate.now()));
        assertThrows(RuntimeException.class, () -> w.pagoService.anular(p.getId(), "  "));
        assertThrows(RuntimeException.class, () -> w.pagoService.anular(p.getId(), "ab"));
        assertEquals(EstadoPago.Pagado, w.pagos.get(0).getEstado());
        w.pagoService.anular(p.getId(), "Motivo valido");
        assertThrows(RuntimeException.class, () -> w.pagoService.anular(p.getId(), "Motivo valido"));
        assertThrows(RuntimeException.class, () -> w.pagoService.actualizarEstado(p.getId(), "Pagado"));
        assertThrows(RuntimeException.class, () -> w.pagoService.actualizarEstado(p.getId(), "Anulado"));
    }

    @Test
    void conLaCajaCerradaNoSeRegistraConfirmaNiAnulaUnPagoPagado() {
        MundoFinanciero w = new MundoFinanciero();
        LocalDate ayer = LocalDate.now().minusDays(1);
        Factura f = w.factura(1, "100000", LocalDate.now().plusDays(5));
        // pago ya confirmado ayer, y luego se cierra la caja de ayer
        Pago pagado = w.pagoDirecto(f, "30000", EstadoPago.Pagado, ayer);
        Pago pendiente = w.pagoDirecto(f, "20000", EstadoPago.Pendiente, ayer);
        w.diasCerrados.add(ayer);

        assertThrows(IllegalStateException.class, () -> w.pagoService.registrarPago(dto(f.getId(), "10000", "Pagado", ayer)));
        assertThrows(IllegalStateException.class, () -> w.pagoService.anular(pagado.getId(), "Correccion tardia"));
        assertThrows(IllegalStateException.class, () -> w.pagoService.actualizarEstado(pendiente.getId(), "Pagado"));
        assertThrows(IllegalStateException.class, () -> w.pagoService.actualizarEstado(pagado.getId(), "Pendiente"));
        assertEquals(EstadoPago.Pagado, pagado.getEstado());
        assertEquals(EstadoPago.Pendiente, pendiente.getEstado());
        assertEquals(2, w.pagos.size());
    }

    @Test
    void conLaCajaCerradaSiSePuedeRegistrarUnPendienteOAnularUnPendiente() {
        // Pendiente/Vencido no son dinero recibido: no afectan el cuadre de caja.
        MundoFinanciero w = new MundoFinanciero();
        LocalDate ayer = LocalDate.now().minusDays(1);
        Factura f = w.factura(1, "100000", LocalDate.now().plusDays(5));
        w.diasCerrados.add(ayer);
        PagoResponseDTO p = w.pagoService.registrarPago(dto(f.getId(), "10000", "Pendiente", ayer));
        PagoResponseDTO a = w.pagoService.anular(p.getId(), "Ya no aplica");
        assertEquals("Anulado", a.getEstado());
    }

    @Test
    void elPagoPagadoDeHoyFuncionaConOtroDiaCerrado() {
        MundoFinanciero w = new MundoFinanciero();
        w.diasCerrados.add(LocalDate.now().minusDays(1));
        Factura f = w.factura(1, "100000", LocalDate.now().plusDays(5));
        PagoResponseDTO r = w.pagoService.registrarPago(dto(f.getId(), "100000", "Pagado", LocalDate.now()));
        assertEquals("Pagado", r.getEstado());
    }

    @Test
    void laReferenciaDelComprobanteSeLimpiaYSeRecortaA100() {
        assertNull(PagoService.recortarReferencia(null));
        assertNull(PagoService.recortarReferencia("   "));
        assertEquals("ABC123", PagoService.recortarReferencia("  ABC123 "));
        assertEquals(100, PagoService.recortarReferencia("x".repeat(150)).length());

        MundoFinanciero w = new MundoFinanciero();
        Factura f = w.factura(1, "100000", LocalDate.now().plusDays(5));
        PagoDTO d = dto(f.getId(), "1000", "Pagado", LocalDate.now());
        d.setReferencia("  NEQUI-998 ");
        w.pagoService.registrarPago(d);
        assertEquals("NEQUI-998", w.pagos.get(0).getReferencia());
    }
}
