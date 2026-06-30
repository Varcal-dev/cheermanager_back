package com.varcal.cheermanager.DTO.Horario_Asistencia;

import java.time.LocalTime;
import java.util.List;

import lombok.Data;

@Data
public class HorarioResponseDTO {
    private Integer id;
    private Integer diaId;
    private String dia;
    private LocalTime horaInicio;
    private LocalTime horaFin;

    // Grupos que entrenan en este bloque de horario (puede ser más de uno,
    // ej. distintos niveles que comparten el mismo horario en días distintos
    // de la semana pero el mismo bloque de hora).
    private List<GrupoAsociado> grupos;

    @Data
    public static class GrupoAsociado {
        private Integer horarioEntrenamientoId;
        private Integer grupoId;
        private String nombreGrupo;
    }
}