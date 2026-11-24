package com.varcal.cheermanager.DTO.Horario_Asistencia;

import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

import lombok.Data;

@Data
public class HorarioDTO {
    @NotNull(message = "El día es obligatorio")
    private Integer diaId;
    private LocalTime horaInicio;
    private LocalTime horaFin;
}