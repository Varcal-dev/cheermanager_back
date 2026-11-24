package com.varcal.cheermanager.DTO.Eventos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

import lombok.Data;

@Data
public class EventoDTO {
    @NotBlank(message = "El nombre del evento es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    private String nombre;
    @NotNull(message = "La fecha del evento es obligatoria")
    private LocalDate fecha;
    @NotNull(message = "El tipo de evento es obligatorio")
    private Integer tipoEventoId;
    @Size(max = 255, message = "La ubicación no puede superar 255 caracteres")
    private String ubicacion;
    private Boolean tieneResultados; // si el productor del evento publica resultados oficiales
}