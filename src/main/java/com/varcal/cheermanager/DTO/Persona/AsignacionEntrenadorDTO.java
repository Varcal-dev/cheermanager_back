package com.varcal.cheermanager.DTO.Persona;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class AsignacionEntrenadorDTO {
    @NotNull(message = "El entrenador es obligatorio")
    private Integer entrenadorId;
    private Integer grupoId;
    @NotBlank(message = "La fecha de inicio es obligatoria")
    private String fechaInicio; // formato ISO (yyyy-MM-dd)
    private String fechaFin;    // puede ser null
    private String rol;
}