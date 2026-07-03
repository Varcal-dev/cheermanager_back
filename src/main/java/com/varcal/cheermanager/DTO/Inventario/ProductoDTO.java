package com.varcal.cheermanager.DTO.Inventario;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductoDTO {
    // "codigo" es solo el segmento COD (3 letras del artículo, ej. CAE) —
    // el prefijo RAT y la categoría se arman en el service a partir de la
    // categoría seleccionada, para no depender de que el usuario escriba el
    // SKU completo a mano y se equivoque de formato.
    private String codigo;
    private String nombre;
    private String descripcion;
    private Integer categoriaProductoId;
    private BigDecimal precioMinReferencia;
    private BigDecimal precioMaxReferencia;
    private String origen;
    private String notasProveedor;
    private Boolean requiereControlStock;
}