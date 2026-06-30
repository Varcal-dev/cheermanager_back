package com.varcal.cheermanager.DTO.Evaluaciones;

import java.time.LocalDate;

import lombok.Data;

@Data
public class EvaluacionResponseDTO {
    private Integer id;
    private Integer deportistaId;
    private String nombreDeportista;
    private Integer criterioId;
    private String nombreCriterio;
    private String categoria;
    private LocalDate fecha;
    private Integer puntajeObtenido;
}