package com.varcal.cheermanager.DTO.Evaluaciones;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CriterioEvaluacionDTO {
    @NotBlank(message = "El nombre del criterio es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    private String nombre;
    @NotNull(message = "Selecciona la categoría")
    private Integer categoriaId;
}