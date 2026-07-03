package com.varcal.cheermanager.DTO.Inventario;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductoVarianteDTO {
    private Integer productoId;
    private String talla;   // nullable
    private String color;   // nullable — para DIG se usa como "tier" (Básico/Competencia/Elite)
    private BigDecimal precioVenta;
    private Integer stockInicial; // se ignora si producto.requiereControlStock = false
}