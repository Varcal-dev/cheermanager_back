package com.varcal.cheermanager.DTO.Evaluaciones;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

import lombok.Data;

@Data
public class EvaluacionDTO {
    @NotNull(message = "Indica el deportista")
    private Integer deportistaId;
    @NotNull(message = "Indica el criterio")
    private Integer criterioId;
    private LocalDate fecha;
    private Integer puntajeObtenido;
}