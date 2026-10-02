package com.varcal.cheermanager.DTO.Persona;

import java.time.LocalDate;

import lombok.Data;

@Data
public class AsignacionEntrenadorResponseDTO {
    private Integer id;
    private Integer grupoId;
    private String grupoNombre;
    private String rol;
    private LocalDate fechaInicio;
    private LocalDate fechaFin; // null = asignación activa
}