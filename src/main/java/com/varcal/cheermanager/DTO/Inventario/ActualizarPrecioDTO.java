package com.varcal.cheermanager.DTO.Inventario;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/**
 * Cuerpo de PUT /api/producto-variantes/{id}/precio.
 * Mantiene la misma forma JSON que antes: {"precioVenta": 25000}.
 */
public record ActualizarPrecioDTO(
        @NotNull(message = "El precio de venta es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio de venta no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El precio de venta admite hasta 8 enteros y 2 decimales")
        BigDecimal precioVenta) {
}
