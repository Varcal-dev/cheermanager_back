package com.varcal.cheermanager.Service.Inventario;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Inventario.ProductoVarianteDTO;
import com.varcal.cheermanager.DTO.Inventario.ProductoVarianteResponseDTO;
import com.varcal.cheermanager.Models.Inventario.Producto;
import com.varcal.cheermanager.Models.Inventario.ProductoVariante;
import com.varcal.cheermanager.repository.Inventario.ProductoRepository;
import com.varcal.cheermanager.repository.Inventario.ProductoVarianteRepository;

@Service
public class ProductoVarianteService {

    @Autowired
    private ProductoVarianteRepository productoVarianteRepository;

    @Autowired
    private ProductoRepository productoRepository;

    public List<ProductoVarianteResponseDTO> listarPorProducto(Integer productoId) {
        return productoVarianteRepository.findByProductoId(productoId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public List<ProductoVarianteResponseDTO> listarConStockBajo(int umbral) {
        return productoVarianteRepository.findConStockBajo(umbral).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public ProductoVariante crear(ProductoVarianteDTO dto) {
        Producto producto = productoRepository.findById(dto.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + dto.getProductoId()));

        if (dto.getPrecioVenta() == null) {
            throw new RuntimeException("La variante necesita un precio de venta");
        }

        ProductoVariante variante = new ProductoVariante();
        variante.setProducto(producto);
        variante.setTalla(dto.getTalla());
        variante.setColor(dto.getColor());
        variante.setPrecioVenta(dto.getPrecioVenta());
        variante.setActivo(true);

        // Secuencia 001, 002... por producto, siguiendo el formato oficial
        // RAT-[CAT]-[COD]-[SEQ] del catálogo. El stock inicial se asigna
        // directo (no genera un MovimientoInventario retroactivo) — desde
        // ahí en adelante, todo cambio de stock sí debe pasar por
        // MovimientoInventarioService para quedar trazado.
        int siguienteSeq = productoVarianteRepository.findByProductoId(producto.getId()).size() + 1;
        variante.setSkuVariante(producto.getSku() + "-" + String.format("%03d", siguienteSeq));

        if (Boolean.TRUE.equals(producto.getRequiereControlStock())) {
            variante.setStockActual(dto.getStockInicial() != null ? dto.getStockInicial() : 0);
        } else {
            variante.setStockActual(null); // artículos digitales (DIG): no se controla stock
        }

        return productoVarianteRepository.save(variante);
    }

    public ProductoVariante actualizarPrecio(Integer id, java.math.BigDecimal nuevoPrecio) {
        ProductoVariante variante = buscarOFallar(id);
        variante.setPrecioVenta(nuevoPrecio);
        return productoVarianteRepository.save(variante);
    }

    public void desactivar(Integer id) {
        ProductoVariante variante = buscarOFallar(id);
        variante.setActivo(false);
        productoVarianteRepository.save(variante);
    }

    private ProductoVariante buscarOFallar(Integer id) {
        return productoVarianteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Variante de producto no encontrada con ID: " + id));
    }

    private ProductoVarianteResponseDTO toDTO(ProductoVariante v) {
        ProductoVarianteResponseDTO dto = new ProductoVarianteResponseDTO();
        dto.setId(v.getId());
        dto.setProductoId(v.getProducto().getId());
        dto.setNombreProducto(v.getProducto().getNombre());
        dto.setTalla(v.getTalla());
        dto.setColor(v.getColor());
        dto.setSkuVariante(v.getSkuVariante());
        dto.setPrecioVenta(v.getPrecioVenta());
        dto.setStockActual(v.getStockActual());
        dto.setActivo(v.getActivo());
        return dto;
    }
}