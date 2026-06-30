package com.varcal.cheermanager.DTO.Horario_Asistencia;

import java.time.LocalDate;

import lombok.Data;

@Data
public class AsistenciaResponseDTO {
    private Integer id;
    private Integer deportistaId;
    private String nombreDeportista;
    private Integer horarioId;
    private LocalDate fecha;
    private Integer estadoAsistenciaId;
    private String estadoAsistencia;
}