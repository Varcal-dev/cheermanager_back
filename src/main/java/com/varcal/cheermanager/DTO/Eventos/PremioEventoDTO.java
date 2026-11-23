package com.varcal.cheermanager.DTO.Eventos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PremioEventoDTO {
    @NotNull(message = "El evento es obligatorio")
    private Integer eventoId;
    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    private String descripcion; // ej. "1er lugar Nivel 3 Youth"
    @Size(max = 255, message = "El premio no puede superar 255 caracteres")
    private String premio;      // ej. "Trofeo + beca 20% próxima temporada"
}