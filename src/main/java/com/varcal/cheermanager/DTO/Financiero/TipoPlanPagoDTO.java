package com.varcal.cheermanager.DTO.Financiero;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class TipoPlanPagoDTO {
    private Integer id;
    @NotBlank(message = "El nombre del tipo de plan es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    private String nombre;
    @NotBlank(message = "La categoría es obligatoria")
    @Size(max = 50, message = "La categoría no puede superar 50 caracteres")
    private String categoria; // "inscripcion" | "mensualidad"
    @NotNull(message = "La duración en meses es obligatoria")
    @Min(value = 1, message = "La duración debe ser de al menos 1 mes")
    private Integer duracionMeses;
    private String descripcion;
    private Long planesAsociados; // solo lectura, informativo en la tabla
}