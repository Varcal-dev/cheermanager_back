package com.varcal.cheermanager.DTO.Inventario;

import lombok.Data;

@Data
public class MovimientoInventarioDTO {
    private Integer productoVarianteId;
    private String tipo; // entrada, salida, ajuste
    private Integer cantidad;
    private String motivo;
}