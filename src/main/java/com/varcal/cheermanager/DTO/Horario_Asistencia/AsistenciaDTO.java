package com.varcal.cheermanager.DTO.Horario_Asistencia;

import java.time.LocalDate;

import lombok.Data;

@Data
public class AsistenciaDTO {
    private Integer deportistaId;
    private Integer horarioId;
    private LocalDate fecha;
    private Integer estadoAsistenciaId;
}