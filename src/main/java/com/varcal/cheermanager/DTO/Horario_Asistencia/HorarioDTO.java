package com.varcal.cheermanager.DTO.Horario_Asistencia;

import java.time.LocalTime;

import lombok.Data;

@Data
public class HorarioDTO {
    private Integer diaId;
    private LocalTime horaInicio;
    private LocalTime horaFin;
}