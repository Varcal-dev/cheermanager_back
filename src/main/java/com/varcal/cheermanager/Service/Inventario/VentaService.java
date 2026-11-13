package com.varcal.cheermanager.Service.Inventario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Inventario.MovimientoInventarioDTO;
import com.varcal.cheermanager.DTO.Inventario.VentaDTO;
import com.varcal.cheermanager.DTO.Inventario.VentaResponseDTO;
import com.varcal.cheermanager.DTO.Financiero.PagoDTO;
import com.varcal.cheermanager.DTO.Financiero.PagoResponseDTO;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.Models.Financiero.MetodoPago;
import com.varcal.cheermanager.Models.Financiero.TipoPago;
import com.varcal.cheermanager.Models.Inventario.DetalleVenta;
import com.varcal.cheermanager.Models.Inventario.ProductoVariante;
import com.varcal.cheermanager.Models.Inventario.Venta;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Financiero.FacturaService;
import com.varcal.cheermanager.Service.Financiero.PagoService;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
import com.varcal.cheermanager.repository.Financiero.MetodoPagoRepository;
import com.varcal.cheermanager.repository.Financiero.TipoPagoRepository;
import com.varcal.cheermanager.repository.Inventario.ProductoVarianteRepository;
import com.varcal.cheermanager.repository.Inventario.VentaRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;

@Service
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoVarianteRepository productoVarianteRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private FacturaService facturaService;

    @Autowired
    private PagoService pagoService;

    @Autowired
    private MetodoPagoRepository metodoPagoRepository;

    @Autowired
    private TipoPagoRepository tipoPagoRepository;

    /** Tipo de pago del catalogo con el que se registran los cobros de tienda. */
    private static final String TIPO_PAGO_TIENDA = "Producto de tienda";

    @Autowired
    private MovimientoInventarioService movimientoInventarioService;

    // Se marca @Transactional porque una venta toca varias tablas (Venta,
    // N DetalleVenta, N MovimientoInventario, N ProductoVariante.stockActual)
    // — si falla a mitad de camino (ej. un ítem sin stock), nada debe quedar
    // guardado a medias.
    @Transactional
    public VentaResponseDTO registrar(VentaDTO dto) {
        validarComprador(dto);

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new RuntimeException("La venta debe tener al menos un ítem");
        }

        Venta venta = new Venta();
        venta.setFecha(com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy());
        venta.setMetodoPago(dto.getMetodoPago());

        if (dto.getPersonaId() != null) {
            Persona persona = personaRepository.findById(dto.getPersonaId())
                    .orElseThrow(() -> new RuntimeException("Persona no encontrada con ID: " + dto.getPersonaId()));
            venta.setPersona(persona);
        } else {
            venta.setNombreCompradorExterno(dto.getNombreCompradorExterno());
            venta.setContactoCompradorExterno(dto.getContactoCompradorExterno());
        }

        if (dto.getFacturaId() != null) {
            Factura factura = facturaRepository.findById(dto.getFacturaId())
                    .orElseThrow(() -> new RuntimeException("Factura no encontrada con ID: " + dto.getFacturaId()));
            venta.setFactura(factura);
        }

        List<DetalleVenta> detalles = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (VentaDTO.ItemVentaDTO item : dto.getItems()) {
            ProductoVariante variante = productoVarianteRepository.findById(item.getProductoVarianteId())
                    .orElseThrow(() -> new RuntimeException("Variante no encontrada con ID: " + item.getProductoVarianteId()));

            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new RuntimeException("Cantidad inválida para '" + variante.getSkuVariante() + "'");
            }

            // Descuenta stock (si el producto lo controla) a través del
            // service de movimientos, para que quede trazado como salida
            // por venta — no se toca stockActual directo desde acá.
            if (Boolean.TRUE.equals(variante.getProducto().getRequiereControlStock())) {
                MovimientoInventarioDTO movDto = new MovimientoInventarioDTO();
                movDto.setProductoVarianteId(variante.getId());
                movDto.setTipo("salida");
                movDto.setCantidad(item.getCantidad());
                movDto.setMotivo("Venta de tienda");
                movimientoInventarioService.registrar(movDto);
            }

            BigDecimal subtotal = variante.getPrecioVenta().multiply(BigDecimal.valueOf(item.getCantidad()));
            total = total.add(subtotal);

            DetalleVenta detalle = new DetalleVenta();
            detalle.setVenta(venta);
            detalle.setProductoVariante(variante);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(variante.getPrecioVenta());
            detalle.setSubtotal(subtotal);
            detalles.add(detalle);
        }

        venta.setTotal(total);
        venta.setDetalles(detalles);
        Venta guardada = ventaRepository.save(venta); // cascade guarda los detalles

        // Si el comprador es un deportista del club (persona != null) y la
        // venta no venía ya ligada a una factura existente (dto.getFacturaId()),
        // se le genera una factura contable real — así toda venta a un
        // miembro del club queda con soporte fiscal, sin importar si se pagó
        // de contado o no. Los compradores externos (nombreCompradorExterno)
        // nunca tienen Persona, así que nunca pueden tener Factura (el campo
        // Factura.persona es obligatorio) — para ellos basta el comprobante
        // imprimible que ya arma el frontend con los datos de la venta.
        if (guardada.getPersona() != null && guardada.getFactura() == null) {
            Factura factura = new Factura();
            factura.setNumeroFactura(facturaService.generarNumeroFacturaVenta());
            factura.setFechaEmision(guardada.getFecha());
            factura.setPersona(guardada.getPersona());
            factura.setDescripcion("Venta de tienda #" + guardada.getId()
                    + " (" + detalles.size() + (detalles.size() == 1 ? " ítem)" : " ítems)"));
            factura.setTotal(total);
            factura.setConcepto("TIENDA");
            factura.setFechaVencimiento(FacturaService.calcularVencimiento(guardada.getFecha()));
            Factura facturaGuardada = facturaRepository.save(factura);


            guardada.setFactura(facturaGuardada);
            guardada = ventaRepository.save(guardada);

            // Venta de contado (por defecto): se registra el pago para que la factura quede PAGADA y no
            // pase a mora a los 10 dias. Con pagada=false se deja a cuenta (factura pendiente).
            if (!Boolean.FALSE.equals(dto.getPagada())) {
                registrarPagoDeContado(facturaGuardada, dto.getMetodoPago(), total, guardada.getFecha());
            }
        }

        return toResponseDTO(guardada);
    }

    /**
     * Registra el pago "Pagado" de una venta de contado reutilizando PagoService (mismas reglas: lock de
     * la factura, sin sobrepago). Corre dentro de la transaccion de registrar(): si algo falla, la venta,
     * los movimientos de inventario y la factura se revierten juntos.
     */
    private void registrarPagoDeContado(Factura factura, String metodoNombre, BigDecimal monto, LocalDate fecha) {
        MetodoPago metodo = buscarMetodoPago(metodoNombre);
        TipoPago tipo = tipoPagoRepository.findAll().stream()
                .filter(t -> TIPO_PAGO_TIENDA.equalsIgnoreCase(t.getTipo()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "No existe el tipo de pago '" + TIPO_PAGO_TIENDA + "' en el catalogo"));

        PagoDTO pago = new PagoDTO();
        pago.setFacturaId(factura.getId());
        pago.setTipoPagoId(tipo.getId());
        pago.setMetodoPagoId(metodo.getId());
        pago.setFecha(fecha);
        pago.setEstado("Pagado");
        pago.setMonto(monto);
        pagoService.registrarPago(pago);
    }

    /** Coincide por nombre exacto o por prefijo: "Transferencia" -> "Transferencia bancaria", "Tarjeta" -> "Tarjeta de credito/debito". */
    private MetodoPago buscarMetodoPago(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new RuntimeException("Indica el metodo de pago de la venta");
        }
        String buscado = nombre.trim().toLowerCase();
        List<MetodoPago> todos = metodoPagoRepository.findAll();
        return todos.stream().filter(m -> m.getMetodo().equalsIgnoreCase(buscado)).findFirst()
                .or(() -> todos.stream().filter(m -> m.getMetodo().toLowerCase().startsWith(buscado)).findFirst())
                .orElseThrow(() -> new RuntimeException("Metodo de pago no reconocido: " + nombre));
    }

    public List<VentaResponseDTO> listarPorRango(LocalDate desde, LocalDate hasta) {
        return ventaRepository.findByFechaBetweenOrderByFechaDesc(desde, hasta).stream()
                .map(this::toResponseDTO).collect(Collectors.toList());
    }

    public List<VentaResponseDTO> listarPorPersona(Integer personaId) {
        return ventaRepository.findByPersonaIdOrderByFechaDesc(personaId).stream()
                .map(this::toResponseDTO).collect(Collectors.toList());
    }

    public VentaResponseDTO obtenerPorId(Integer id) {
        return toResponseDTO(ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + id)));
    }

    // No hay "eliminar" venta: se ANULA (permiso anular_venta). La anulacion reversa todo lo que la venta
    // hizo, sin borrar nada, para no perder el rastro contable:
    //  - stock: un movimiento de ENTRADA por cada linea de productos que controlan stock;
    //  - cobro: si la venta creo su propia factura de tienda, se anulan sus pagos y la factura;
    //    si la venta se cargo a una factura ya existente (facturaId), esa factura NO se toca.
    // Todo en una sola transaccion: o se reversa completo o no se reversa nada.
    @Transactional
    public VentaResponseDTO anular(Integer id, String motivo) {
        String m = FacturaService.validarMotivo(motivo);
        // Lock: dos anulaciones simultaneas no pueden devolver el stock dos veces.
        Venta venta = ventaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + id));
        if (Boolean.TRUE.equals(venta.getAnulada())) {
            throw new RuntimeException("La venta #" + id + " ya esta anulada");
        }

        Factura factura = venta.getFactura();
        if (esFacturaPropia(venta, factura) && !Boolean.TRUE.equals(factura.getAnulada())) {
            String motivoCobro = "Venta #" + id + " anulada: " + m;
            for (PagoResponseDTO p : pagoService.listarPorFactura(factura.getId())) {
                if (!"Anulado".equals(p.getEstado())) {
                    pagoService.anular(p.getId(), motivoCobro);
                }
            }
            facturaService.anular(factura.getId(), motivoCobro);
        }

        if (venta.getDetalles() != null) {
            for (DetalleVenta d : venta.getDetalles()) {
                ProductoVariante variante = d.getProductoVariante();
                if (Boolean.TRUE.equals(variante.getProducto().getRequiereControlStock())) {
                    MovimientoInventarioDTO mov = new MovimientoInventarioDTO();
                    mov.setProductoVarianteId(variante.getId());
                    mov.setTipo("entrada");
                    mov.setCantidad(d.getCantidad());
                    mov.setMotivo("Anulación de venta #" + id);
                    movimientoInventarioService.registrar(mov);
                }
            }
        }

        venta.setAnulada(true);
        venta.setMotivoAnulacion(m);
        venta.setFechaAnulacion(java.time.LocalDateTime.now());
        venta.setAnuladaPorUsuarioId(FacturaService.usuarioActualId());
        return toResponseDTO(ventaRepository.save(venta));
    }

    /** La factura la creo esta venta (concepto TIENDA y descripcion "Venta de tienda #<id> (...)"), no es una cuenta ajena. */
    private boolean esFacturaPropia(Venta venta, Factura factura) {
        return factura != null
                && "TIENDA".equals(factura.getConcepto())
                && factura.getDescripcion() != null
                && factura.getDescripcion().startsWith("Venta de tienda #" + venta.getId() + " (");
    }

    private void validarComprador(VentaDTO dto) {
        boolean tienePersona = dto.getPersonaId() != null;
        boolean tieneExterno = dto.getNombreCompradorExterno() != null && !dto.getNombreCompradorExterno().isBlank();

        if (tienePersona == tieneExterno) {
            throw new RuntimeException(
                    "La venta debe tener exactamente un comprador: o una persona del club (personaId) o un comprador externo (nombreCompradorExterno), no ambos ni ninguno");
        }
    }

    private VentaResponseDTO toResponseDTO(Venta v) {
        VentaResponseDTO dto = new VentaResponseDTO();
        dto.setId(v.getId());
        dto.setFecha(v.getFecha());
        dto.setTotal(v.getTotal());
        dto.setMetodoPago(v.getMetodoPago());
        dto.setFacturaId(v.getFactura() != null ? v.getFactura().getId() : null);
        dto.setNumeroFactura(v.getFactura() != null ? v.getFactura().getNumeroFactura() : null);
        dto.setAnulada(Boolean.TRUE.equals(v.getAnulada()));
        dto.setMotivoAnulacion(v.getMotivoAnulacion());
        dto.setFechaAnulacion(v.getFechaAnulacion());

        if (v.getPersona() != null) {
            dto.setPersonaId(v.getPersona().getId());
            dto.setNombreComprador(v.getPersona().getNombre() + " " + v.getPersona().getApellidos());
        } else {
            dto.setNombreComprador(v.getNombreCompradorExterno());
        }

        if (v.getDetalles() != null) {
            dto.setDetalles(v.getDetalles().stream().map(d -> {
                VentaResponseDTO.DetalleVentaResponseDTO det = new VentaResponseDTO.DetalleVentaResponseDTO();
                det.setProductoVarianteId(d.getProductoVariante().getId());
                det.setSkuVariante(d.getProductoVariante().getSkuVariante());
                det.setNombreProducto(d.getProductoVariante().getProducto().getNombre());
                det.setTalla(d.getProductoVariante().getTalla());
                det.setColor(d.getProductoVariante().getColor());
                det.setCantidad(d.getCantidad());
                det.setPrecioUnitario(d.getPrecioUnitario());
                det.setSubtotal(d.getSubtotal());
                return det;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}