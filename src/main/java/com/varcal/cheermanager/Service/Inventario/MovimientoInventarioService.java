package com.varcal.cheermanager.Service.Inventario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Inventario.MovimientoInventarioDTO;
import com.varcal.cheermanager.DTO.Inventario.MovimientoInventarioResponseDTO;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Inventario.MovimientoInventario;
import com.varcal.cheermanager.Models.Inventario.MovimientoInventario.TipoMovimiento;
import com.varcal.cheermanager.Models.Inventario.ProductoVariante;
import com.varcal.cheermanager.Utils.AuthenticatedUserUtil;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Inventario.MovimientoInventarioRepository;
import com.varcal.cheermanager.repository.Inventario.ProductoVarianteRepository;

@Service
public class MovimientoInventarioService {

    @Autowired
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @Autowired
    private ProductoVarianteRepository productoVarianteRepository;

    @Autowired
    private UserRepository userRepository;

    // Único método del sistema que debe modificar ProductoVariante.stockActual
    // — cualquier ajuste de stock (venta, compra, conteo físico) pasa por
    // acá, para que MovimientoInventario sea siempre el registro fiel de por
    // qué cambió cada unidad.
    @Transactional
    public MovimientoInventario registrar(MovimientoInventarioDTO dto) {
        ProductoVariante variante = productoVarianteRepository.findById(dto.getProductoVarianteId())
                .orElseThrow(() -> new RuntimeException("Variante no encontrada con ID: " + dto.getProductoVarianteId()));

        if (!Boolean.TRUE.equals(variante.getProducto().getRequiereControlStock())) {
            throw new RuntimeException(
                    "'" + variante.getProducto().getNombre() + "' es un artículo digital y no controla stock");
        }

        if (dto.getCantidad() == null || dto.getCantidad() <= 0) {
            throw new RuntimeException("La cantidad del movimiento debe ser mayor a cero");
        }

        TipoMovimiento tipo;
        try {
            tipo = TipoMovimiento.valueOf(dto.getTipo());
        } catch (Exception e) {
            throw new RuntimeException("Tipo de movimiento inválido. Debe ser: entrada, salida o ajuste");
        }

        Usuario usuario = userRepository.findById(AuthenticatedUserUtil.getCurrentUserId())
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado en base de datos"));

        int stockActual = variante.getStockActual() != null ? variante.getStockActual() : 0;
        int nuevoStock;
        switch (tipo) {
            case entrada -> nuevoStock = stockActual + dto.getCantidad();
            case salida -> {
                nuevoStock = stockActual - dto.getCantidad();
                if (nuevoStock < 0) {
                    throw new RuntimeException(
                            "Stock insuficiente: quedan " + stockActual + " unidades de '" + variante.getSkuVariante() + "'");
                }
            }
            // "ajuste" fija el stock al valor exacto informado en `cantidad`
            // (para cuadrar tras un conteo físico), en vez de sumar/restar.
            case ajuste -> nuevoStock = dto.getCantidad();
            default -> throw new RuntimeException("Tipo de movimiento inválido");
        }

        variante.setStockActual(nuevoStock);
        productoVarianteRepository.save(variante);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProductoVariante(variante);
        movimiento.setTipo(tipo);
        movimiento.setCantidad(dto.getCantidad());
        movimiento.setMotivo(dto.getMotivo());
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setUsuario(usuario);
        return movimientoInventarioRepository.save(movimiento);
    }

    public List<MovimientoInventarioResponseDTO> listarPorVariante(Integer productoVarianteId) {
        return movimientoInventarioRepository.findByProductoVarianteIdOrderByFechaDesc(productoVarianteId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    private MovimientoInventarioResponseDTO toDTO(MovimientoInventario m) {
        MovimientoInventarioResponseDTO dto = new MovimientoInventarioResponseDTO();
        dto.setId(m.getId());
        dto.setProductoVarianteId(m.getProductoVariante().getId());
        dto.setSkuVariante(m.getProductoVariante().getSkuVariante());
        dto.setNombreProducto(m.getProductoVariante().getProducto().getNombre());
        dto.setTipo(m.getTipo().name());
        dto.setCantidad(m.getCantidad());
        dto.setMotivo(m.getMotivo());
        dto.setFecha(m.getFecha());
        dto.setUsuario(m.getUsuario().getUsername());
        return dto;
    }
}