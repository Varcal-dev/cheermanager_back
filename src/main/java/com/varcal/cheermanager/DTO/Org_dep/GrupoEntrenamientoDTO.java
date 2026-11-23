package com.varcal.cheermanager.DTO.Org_dep;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GrupoEntrenamientoDTO {
    @NotBlank(message = "El nombre del grupo es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    private String nombre;
    @NotNull(message = "El tipo de grupo es obligatorio")
    private Integer tipoGrupoId;
    @NotNull(message = "La categoría de nivel es obligatoria")
    private Integer categoriaNivelId;

    // Getters y setters
}