package com.varcal.cheermanager.DTO.Horario_Asistencia;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class AsignarHorarioGrupoDTO {
    @NotNull(message = "El horario es obligatorio")
    private Integer horarioId;
    @NotNull(message = "El grupo es obligatorio")
    private Integer grupoEntrenamientoId;
}