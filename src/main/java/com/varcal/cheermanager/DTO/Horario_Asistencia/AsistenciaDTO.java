package com.varcal.cheermanager.DTO.Horario_Asistencia;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

import lombok.Data;

@Data
public class AsistenciaDTO {
    @NotNull(message = "Indica el deportista")
    private Integer deportistaId;
    @NotNull(message = "Indica el horario")
    private Integer horarioId;
    @NotNull(message = "Indica la fecha del entrenamiento")
    private LocalDate fecha;
    @NotNull(message = "Indica el estado de asistencia")
    private Integer estadoAsistenciaId;
}