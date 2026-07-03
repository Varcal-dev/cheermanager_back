package com.varcal.cheermanager.DTO.Inventario;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MovimientoInventarioResponseDTO {
    private Integer id;
    private Integer productoVarianteId;
    private String skuVariante;
    private String nombreProducto;
    private String tipo;
    private Integer cantidad;
    private String motivo;
    private LocalDateTime fecha;
    private String usuario;
}