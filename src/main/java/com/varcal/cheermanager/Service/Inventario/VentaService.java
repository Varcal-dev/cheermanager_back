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
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.Models.Inventario.DetalleVenta;
import com.varcal.cheermanager.Models.Inventario.ProductoVariante;
import com.varcal.cheermanager.Models.Inventario.Venta;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Financiero.FacturaService;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
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
        venta.setFecha(LocalDate.now());
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
        }

        return toResponseDTO(guardada);
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

    // Nota: no hay "eliminar" venta — anular_venta (permiso ya existe en el
    // seed) debería implementarse como una reversión que genera movimientos
    // de entrada compensatorios, no un borrado, para no perder el rastro
    // contable. Se deja pendiente hasta que se defina esa regla de negocio.

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