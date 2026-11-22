package com.varcal.cheermanager.DTO.Inventario;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductoVarianteDTO {
    @NotNull(message = "El producto es obligatorio")
    private Integer productoId;
    @Size(max = 20, message = "La talla no puede superar 20 caracteres")
    private String talla;   // nullable
    @Size(max = 30, message = "El color no puede superar 30 caracteres")
    private String color;   // nullable — para DIG se usa como "tier" (Básico/Competencia/Elite)
    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio de venta no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El precio de venta admite hasta 8 enteros y 2 decimales")
    private BigDecimal precioVenta;
    @PositiveOrZero(message = "El stock inicial no puede ser negativo")
    private Integer stockInicial; // se ignora si producto.requiereControlStock = false
}