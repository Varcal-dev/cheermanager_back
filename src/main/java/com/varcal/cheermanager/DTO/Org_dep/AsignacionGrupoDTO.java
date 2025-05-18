package com.varcal.cheermanager.DTO.Org_dep;

import java.time.LocalDate;

import lombok.Data;

@Data
public class AsignacionGrupoDTO {
    private Long deportistaId;
    private Long grupoId;
    private LocalDate fechaInicio;
    private String observaciones;
    // Getters y setters
}
