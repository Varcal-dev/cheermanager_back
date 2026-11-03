package com.varcal.cheermanager.DTO.Evaluaciones;

import java.time.LocalDate;

import lombok.Data;

/** Correccion de una evaluacion ya registrada: solo puntaje y fecha (el deportista y el criterio no cambian). */
@Data
public class EvaluacionActualizarDTO {
    private LocalDate fecha;
    private Integer puntajeObtenido;
}
