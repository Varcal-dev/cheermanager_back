package com.varcal.cheermanager.DTO.Inventario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoriaProductoDTO {
    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    private String nombre;
    @NotBlank(message = "El código de la categoría es obligatorio")
    @Size(max = 3, message = "El código debe tener máximo 3 letras")
    private String codigo; // 3 letras, ej. ROP, CAL, DIG
    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    private String descripcion;
}