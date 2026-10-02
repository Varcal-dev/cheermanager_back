package com.varcal.cheermanager.DTO.Ath;

import java.util.List;

import lombok.Data;

@Data
public class CrearRolConPermisosDTO {
    private Integer rolId;
    private String nombre;
    private List<Integer> permisoIds;
}
