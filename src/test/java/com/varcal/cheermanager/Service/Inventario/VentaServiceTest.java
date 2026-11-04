package com.varcal.cheermanager.Service.Inventario;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.DTO.Inventario.MovimientoInventarioDTO;
import com.varcal.cheermanager.DTO.Inventario.VentaDTO;
import com.varcal.cheermanager.DTO.Inventario.VentaResponseDTO;
import com.varcal.cheermanager.Models.Financiero.EstadoPago;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.Models.Financiero.MetodoPago;
import com.varcal.cheermanager.Models.Financiero.TipoPago;
import com.varcal.cheermanager.Models.Inventario.Producto;
import com.varcal.cheermanager.Models.Inventario.ProductoVariante;
import com.varcal.cheermanager.Models.Inventario.Venta;
import com.varcal.cheermanager.Models.Inventario.MovimientoInventario;
import com.varcal.cheermanager.repository.Financiero.MetodoPagoRepository;
import com.varcal.cheermanager.repository.Financiero.TipoPagoRepository;
import com.varcal.cheermanager.repository.Inventario.ProductoVarianteRepository;
import com.varcal.cheermanager.repository.Inventario.VentaRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;
import com.varcal.cheermanager.testsupport.MundoFinanciero;

/** Ventas de tienda: factura + pago de contado, y anulacion que reversa stock y cobro. Sin base de datos. */
class VentaServiceTest {

    /** Inventario falso: aplica la entrada/salida al stock de la variante y deja el registro de movimientos. */
    static class InventarioFalso extends MovimientoInventarioService {
        final List<String> movimientos = new ArrayList<>();
        final List<ProductoVariante> variantes;
        InventarioFalso(List<ProductoVariante> variantes) { this.variantes = variantes; }
        @Override
        public MovimientoInventario registrar(MovimientoInventarioDTO dto) {
            ProductoVariante v = variantes.stream().filter(x -> x.getId().equals(dto.getProductoVarianteId())).findFirst().get();
            int delta = "entrada".equals(dto.getTipo()) ? dto.getCantidad() : -dto.getCantidad();
            if (v.getStockActual() + delta < 0) throw new RuntimeException("Stock insuficiente para " + v.getSkuVariante());
            v.setStockActual(v.getStockActual() + delta);
            movimientos.add(dto.getTipo() + ":" + v.getId() + ":" + dto.getCantidad());
            return null;
        }
    }

    static class Tienda {
        final MundoFinanciero w = new MundoFinanciero();
        final List<Venta> ventas = new ArrayList<>();
        final List<ProductoVariante> variantes = new ArrayList<>();
        final InventarioFalso inventario;
        final VentaService servicio = new VentaService();
        int seq = 1;

        Tienda() {
            variantes.add(variante(1, "UNI-S", "35000", 10, true));   // uniforme: controla stock
            variantes.add(variante(2, "LAZO", "5000", 0, false));     // servicio/lazo: no controla stock
            inventario = new InventarioFalso(variantes);
            w.persona(1, "Ana");

            Repo ventaRepo = new Repo()
                    .on("save", a -> {
                        Venta v = (Venta) a[0];
                        if (v.getId() == null) { v.setId(seq++); ventas.add(v); }
                        return v;
                    })
                    .on("findById", a -> ventas.stream().filter(v -> v.getId().equals(a[0])).findFirst())
                    .on("findByIdForUpdate", a -> ventas.stream().filter(v -> v.getId().equals(a[0])).findFirst());
            Repo varianteRepo = new Repo().on("findById",
                    a -> variantes.stream().filter(v -> v.getId().equals(a[0])).findFirst());
            Repo metodoRepo = new Repo().on("findAll", a -> {
                MetodoPago m = new MetodoPago(); m.setId(1); m.setMetodo("Efectivo");
                MetodoPago t = new MetodoPago(); t.setId(2); t.setMetodo("Transferencia bancaria");
                return List.of(m, t);
            });
            Repo tipoRepo = new Repo().on("findAll", a -> {
                TipoPago t = new TipoPago(); t.setId(9); t.setTipo("Producto de tienda");
                return List.of(t);
            });
            Fakes.inyectar(servicio, "ventaRepository", Fakes.proxy(VentaRepository.class, ventaRepo));
            Fakes.inyectar(servicio, "productoVarianteRepository", Fakes.proxy(ProductoVarianteRepository.class, varianteRepo));
            Fakes.inyectar(servicio, "personaRepository", w.personaRepository);
            Fakes.inyectar(servicio, "facturaRepository", w.facturaRepository);
            Fakes.inyectar(servicio, "facturaService", w.facturaService);
            Fakes.inyectar(servicio, "pagoService", w.pagoService);
            Fakes.inyectar(servicio, "metodoPagoRepository", Fakes.proxy(MetodoPagoRepository.class, metodoRepo));
            Fakes.inyectar(servicio, "tipoPagoRepository", Fakes.proxy(TipoPagoRepository.class, tipoRepo));
            Fakes.inyectar(servicio, "movimientoInventarioService", inventario);
        }

        static ProductoVariante variante(int id, String sku, String precio, int stock, boolean controla) {
            Producto p = new Producto();
            p.setNombre("Producto " + sku);
            p.setRequiereControlStock(controla);
            ProductoVariante v = new ProductoVariante();
            v.setId(id);
            v.setSkuVariante(sku);
            v.setProducto(p);
            v.setPrecioVenta(new BigDecimal(precio));
            v.setStockActual(stock);
            return v;
        }

        VentaDTO ventaAPersona(Boolean pagada, int... varianteYCantidad) {
            VentaDTO d = new VentaDTO();
            d.setPersonaId(1);
            d.setMetodoPago("Efectivo");
            d.setPagada(pagada);
            List<VentaDTO.ItemVentaDTO> items = new ArrayList<>();
            for (int i = 0; i < varianteYCantidad.length; i += 2) {
                VentaDTO.ItemVentaDTO it = new VentaDTO.ItemVentaDTO();
                it.setProductoVarianteId(varianteYCantidad[i]);
                it.setCantidad(varianteYCantidad[i + 1]);
                items.add(it);
            }
            d.setItems(items);
            return d;
        }
    }

    @Test
    void ventaDeContadoACliente_creaFacturaYPagoYBajaStock() {
        Tienda t = new Tienda();
        VentaResponseDTO r = t.servicio.registrar(t.ventaAPersona(null, 1, 2, 2, 1)); // 2x35000 + 1x5000
        assertTrue(Fakes.igual("75000", r.getTotal()));
        assertEquals(8, (int) t.variantes.get(0).getStockActual());
        assertEquals(0, (int) t.variantes.get(1).getStockActual()); // no controla stock: no se mueve
        assertEquals(1, t.inventario.movimientos.size());

        Factura f = t.w.facturas.get(0);
        assertEquals("TIENDA", f.getConcepto());
        assertTrue(f.getNumeroFactura().startsWith("FAC-VTA-"));
        assertEquals(1, t.w.pagos.size());
        assertEquals(EstadoPago.Pagado, t.w.pagos.get(0).getEstado());
        assertEquals("PAGADA", t.w.facturaService.toResponseDTO(f).getEstadoCuenta());
    }

    @Test
    void pagadaFalseDejaLaFacturaPendienteSinPago() {
        Tienda t = new Tienda();
        t.servicio.registrar(t.ventaAPersona(false, 1, 1));
        assertEquals(0, t.w.pagos.size());
        assertEquals("PENDIENTE", t.w.facturaService.toResponseDTO(t.w.facturas.get(0)).getEstadoCuenta());
    }

    @Test
    void compradorExternoNoGeneraFactura() {
        Tienda t = new Tienda();
        VentaDTO d = t.ventaAPersona(null, 1, 1);
        d.setPersonaId(null);
        d.setNombreCompradorExterno("Visitante");
        VentaResponseDTO r = t.servicio.registrar(d);
        assertNull(r.getFacturaId());
        assertEquals(0, t.w.facturas.size());
        assertEquals(0, t.w.pagos.size());
    }

    @Test
    void validaCompradorItemsCantidadYMetodoDePago() {
        Tienda t = new Tienda();
        VentaDTO ambos = t.ventaAPersona(null, 1, 1);
        ambos.setNombreCompradorExterno("X");
        assertThrows(RuntimeException.class, () -> t.servicio.registrar(ambos));
        VentaDTO ninguno = t.ventaAPersona(null, 1, 1);
        ninguno.setPersonaId(null);
        assertThrows(RuntimeException.class, () -> t.servicio.registrar(ninguno));
        VentaDTO sinItems = t.ventaAPersona(null);
        assertThrows(RuntimeException.class, () -> t.servicio.registrar(sinItems));
        assertThrows(RuntimeException.class, () -> t.servicio.registrar(t.ventaAPersona(null, 1, 0)));
        assertThrows(RuntimeException.class, () -> t.servicio.registrar(t.ventaAPersona(null, 99, 1)));
        VentaDTO metodoMalo = t.ventaAPersona(null, 1, 1);
        metodoMalo.setMetodoPago("Trueque");
        assertThrows(RuntimeException.class, () -> t.servicio.registrar(metodoMalo));
    }

    @Test
    void metodoDePagoSeReconocePorPrefijo() {
        Tienda t = new Tienda();
        VentaDTO d = t.ventaAPersona(null, 1, 1);
        d.setMetodoPago("transferencia");
        t.servicio.registrar(d);
        assertEquals(2, (int) t.w.pagos.get(0).getMetodoPago().getId());
    }

    @Test
    void stockInsuficienteFallaYNoSeCobraNada() {
        Tienda t = new Tienda();
        assertThrows(RuntimeException.class, () -> t.servicio.registrar(t.ventaAPersona(null, 1, 11)));
        assertEquals(0, t.w.facturas.size());
        assertEquals(0, t.w.pagos.size());
        assertEquals(10, (int) t.variantes.get(0).getStockActual());
    }

    @Test
    void anularVentaDevuelveStockAnulaPagoYFactura() {
        Tienda t = new Tienda();
        VentaResponseDTO r = t.servicio.registrar(t.ventaAPersona(null, 1, 3));
        assertEquals(7, (int) t.variantes.get(0).getStockActual());

        VentaResponseDTO a = t.servicio.anular(r.getId(), "Cliente devolvio todo");
        assertTrue(a.getAnulada());
        assertEquals(10, (int) t.variantes.get(0).getStockActual());
        assertEquals(EstadoPago.Anulado, t.w.pagos.get(0).getEstado());
        assertTrue(t.w.facturas.get(0).getAnulada());
        assertEquals("ANULADA", t.w.facturaService.toResponseDTO(t.w.facturas.get(0)).getEstadoCuenta());
        // nada se borra
        assertEquals(1, t.w.facturas.size());
        assertEquals(1, t.w.pagos.size());
    }

    @Test
    void anularDosVecesNoDevuelveElStockDosVeces() {
        Tienda t = new Tienda();
        VentaResponseDTO r = t.servicio.registrar(t.ventaAPersona(null, 1, 3));
        t.servicio.anular(r.getId(), "Primera anulacion");
        assertThrows(RuntimeException.class, () -> t.servicio.anular(r.getId(), "Segunda anulacion"));
        assertEquals(10, (int) t.variantes.get(0).getStockActual());
        assertEquals(2, t.inventario.movimientos.size()); // una salida y una entrada
    }

    @Test
    void anularExigeMotivoYNoCambiaNada() {
        Tienda t = new Tienda();
        VentaResponseDTO r = t.servicio.registrar(t.ventaAPersona(null, 1, 3));
        assertThrows(RuntimeException.class, () -> t.servicio.anular(r.getId(), " "));
        assertThrows(RuntimeException.class, () -> t.servicio.anular(404, "No existe esa venta"));
        assertEquals(7, (int) t.variantes.get(0).getStockActual());
        assertEquals(EstadoPago.Pagado, t.w.pagos.get(0).getEstado());
    }

    @Test
    void anularVentaCargadaAFacturaAjenaNoTocaEsaFactura() {
        Tienda t = new Tienda();
        // El deportista ya tenia una mensualidad pendiente; la venta se carga a esa cuenta.
        Factura ajena = t.w.factura(1, "90000", java.time.LocalDate.now().plusDays(3));
        ajena.setConcepto("MENSUALIDAD");
        ajena.setDescripcion("Mensualidad noviembre");
        VentaDTO d = t.ventaAPersona(null, 1, 1);
        d.setFacturaId(ajena.getId());
        VentaResponseDTO r = t.servicio.registrar(d);
        assertEquals(1, t.w.facturas.size()); // no creo factura propia
        assertEquals(0, t.w.pagos.size());

        t.servicio.anular(r.getId(), "Venta cargada por error");
        assertFalse(Boolean.TRUE.equals(ajena.getAnulada()));
        assertEquals(10, (int) t.variantes.get(0).getStockActual());
    }

    @Test
    void anularVentaConPagoParcialAnulaLosPagosPendientesTambien() {
        Tienda t = new Tienda();
        VentaResponseDTO r = t.servicio.registrar(t.ventaAPersona(false, 1, 1)); // a cuenta
        Factura f = t.w.facturas.get(0);
        t.w.pagoDirecto(f, "10000", EstadoPago.Pendiente, java.time.LocalDate.now());
        t.servicio.anular(r.getId(), "Se cancela el pedido");
        assertEquals(EstadoPago.Anulado, t.w.pagos.get(0).getEstado());
        assertTrue(f.getAnulada());
    }

    @Test
    void anularConCajaCerradaDelPagoBloqueaTodaLaAnulacion() {
        Tienda t = new Tienda();
        VentaResponseDTO r = t.servicio.registrar(t.ventaAPersona(null, 1, 2));
        t.w.diasCerrados.add(java.time.LocalDate.now());
        assertThrows(IllegalStateException.class, () -> t.servicio.anular(r.getId(), "Fuera de tiempo"));
        // nada quedo a medias
        assertEquals(8, (int) t.variantes.get(0).getStockActual());
        assertEquals(EstadoPago.Pagado, t.w.pagos.get(0).getEstado());
        assertFalse(t.w.facturas.get(0).getAnulada());
    }
}
