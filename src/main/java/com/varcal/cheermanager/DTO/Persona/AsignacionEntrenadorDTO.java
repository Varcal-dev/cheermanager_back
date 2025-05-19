package com.varcal.cheermanager.DTO.Persona;

import lombok.Data;

@Data
public class AsignacionEntrenadorDTO {
    private Integer entrenadorId;
    private Integer grupoId;
    private String fechaInicio; // formato ISO (yyyy-MM-dd)
    private String fechaFin;    // puede ser null
    private String rol;
}
