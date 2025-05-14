package com.varcal.cheermanager.DTO.Org_dep;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GrupoEntrenamientoDTO {
    private String nombre;
    private Integer tipoGrupoId;
    private Integer categoriaNivelId;

    // Getters y setters
}

