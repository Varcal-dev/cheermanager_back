package com.varcal.cheermanager.DTO.Evaluaciones;

import java.time.LocalDate;

import lombok.Data;

@Data
public class EvaluacionDTO {
    private Integer deportistaId;
    private Integer criterioId;
    private LocalDate fecha;
    private Integer puntajeObtenido;
}