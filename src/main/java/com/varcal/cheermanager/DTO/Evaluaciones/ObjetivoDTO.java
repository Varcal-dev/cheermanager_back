package com.varcal.cheermanager.DTO.Evaluaciones;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

import lombok.Data;

@Data
public class ObjetivoDTO {
    @NotNull(message = "El deportista es obligatorio")
    private Integer deportistaId;
    @NotBlank(message = "El nombre del objetivo es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    private String nombre;
    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    private String descripcion;
    private LocalDate fechaCreacion;
    @NotNull(message = "El estado del objetivo es obligatorio")
    private Integer estadoObjetivoId;
}