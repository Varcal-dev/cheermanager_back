package com.varcal.cheermanager.DTO.Inventario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class VentaResponseDTO {
    private Integer id;
    private LocalDate fecha;
    private BigDecimal total;
    private String metodoPago;
    private Integer personaId;
    private String nombreComprador; // nombre de la Persona, o el externo si aplica
    private Integer facturaId;
    private String numeroFactura; // ej. "FAC-VTA-2026-0001" — null si la venta no generó factura (comprador externo)
    private List<DetalleVentaResponseDTO> detalles;

    @Data
    public static class DetalleVentaResponseDTO {
        private Integer productoVarianteId;
        private String skuVariante;
        private String nombreProducto;
        private String talla;
        private String color;
        private Integer cantidad;
        private BigDecimal precioUnitario;
        private BigDecimal subtotal;
    }
}