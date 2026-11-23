package com.varcal.cheermanager.DTO.Eventos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

import lombok.Data;

@Data
public class ResultadoCompetenciaDTO {
    @NotNull(message = "El evento es obligatorio")
    private Integer eventoId;
    @NotNull(message = "El grupo es obligatorio")
    private Integer grupoEntrenamientoId;
    @Min(value = 1, message = "La posición debe ser 1 o mayor")
    private Integer posicion;
    // Porcentaje: puntos obtenidos segun la rubrica (prep o elite) sobre el total posible, de 0 a 100.
    @DecimalMin(value = "0.00", message = "El puntaje no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El puntaje es un porcentaje: máximo 100")
    @Digits(integer = 3, fraction = 2, message = "El puntaje admite hasta 2 decimales")
    private BigDecimal puntaje;
    private Integer premioId;           // opcional
    private Integer evaluacionRutinaId; // opcional, si el evento se evaluó dentro del sistema
    @Size(max = 255, message = "Las observaciones no puede superar 255 caracteres")
    private String observaciones;
}