package com.varcal.cheermanager.DTO.Inventario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class MovimientoInventarioDTO {
    @NotNull(message = "Indica la variante del producto")
    private Integer productoVarianteId;
    @NotBlank(message = "Indica el tipo de movimiento (entrada, salida o ajuste)")
    private String tipo; // entrada, salida, ajuste
    @NotNull(message = "Indica la cantidad")
    @Positive(message = "La cantidad del movimiento debe ser mayor a cero")
    private Integer cantidad;
    @Size(max = 300, message = "El motivo admite maximo 300 caracteres")
    private String motivo;
}