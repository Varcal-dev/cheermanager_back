package com.varcal.cheermanager.DTO.Inventario;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductoVarianteResponseDTO {
    private Integer id;
    private Integer productoId;
    private String nombreProducto;
    private String talla;
    private String color;
    private String skuVariante;
    private BigDecimal precioVenta;
    private Integer stockActual; // null si el producto no controla stock
    private Boolean activo;
}