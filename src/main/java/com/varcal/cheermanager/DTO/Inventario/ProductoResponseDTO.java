package com.varcal.cheermanager.DTO.Inventario;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class ProductoResponseDTO {
    private Integer id;
    private String sku;
    private String nombre;
    private String descripcion;
    private Integer categoriaProductoId;
    private String categoriaNombre;
    private BigDecimal precioMinReferencia;
    private BigDecimal precioMaxReferencia;
    private String origen;
    private String notasProveedor;
    private Boolean requiereControlStock;
    private Boolean activo;
    private Integer stockTotal; // suma de stock de todas sus variantes (null si no controla stock)
    private List<ProductoVarianteResponseDTO> variantes;
}