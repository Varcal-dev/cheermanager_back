package com.varcal.cheermanager.DTO.Inventario;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProductoDTO {
    // "codigo" es solo el segmento COD (3 letras del artículo, ej. CAE) —
    // el prefijo RAT y la categoría se arman en el service a partir de la
    // categoría seleccionada, para no depender de que el usuario escriba el
    // SKU completo a mano y se equivoque de formato.
    @Size(max = 20, message = "El código no puede superar 20 caracteres")
    private String codigo;
    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    private String nombre;
    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    private String descripcion;
    @NotNull(message = "Selecciona una categoría")
    private Integer categoriaProductoId;
    @DecimalMin(value = "0.00", message = "El precio mínimo de referencia no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El precio mínimo de referencia admite hasta 8 enteros y 2 decimales")
    private BigDecimal precioMinReferencia;
    @DecimalMin(value = "0.00", message = "El precio máximo de referencia no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El precio máximo de referencia admite hasta 8 enteros y 2 decimales")
    private BigDecimal precioMaxReferencia;
    @Size(max = 255, message = "El origen no puede superar 255 caracteres")
    private String origen;
    @Size(max = 500, message = "Las notas del proveedor no pueden superar 500 caracteres")
    private String notasProveedor;
    private Boolean requiereControlStock;
}