package com.varcal.cheermanager.DTO.Ath;

import jakarta.validation.constraints.NotNull;

import java.util.List;

import lombok.Data;

@Data
public class CrearRolConPermisosDTO {
    @NotNull(message = "El rol es obligatorio")
    private Integer rolId;
    private String nombre;
    @NotNull(message = "La lista de permisos es obligatoria (puede ir vacía)")
    private List<Integer> permisoIds;
}