package com.varcal.cheermanager.DTO.Persona;

import java.time.LocalTime;

import lombok.Data;

// Representa un bloque de horario (día/hora) del grupo al que este
// entrenador está asignado activamente, junto con su rol en ese grupo.
// "asignacionId" apunta al GrupoEntrenador — Editar/Eliminar en este panel
// actúan sobre esa asignación (el rol del entrenador en el grupo), no sobre
// el horario en sí, que es un recurso compartido por todo el grupo y se
// edita desde el módulo Horarios.
@Data
public class HorarioEntrenadorResponseDTO {
    private Integer asignacionId;
    private Integer grupoId;
    private String grupoNombre;
    private String rol;
    private String dia;
    private LocalTime horaInicio;
    private LocalTime horaFin;
}