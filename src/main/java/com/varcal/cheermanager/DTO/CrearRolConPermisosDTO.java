package com.varcal.cheermanager.DTO;

import java.util.List;

import lombok.Data;

@Data
public class CrearRolConPermisosDTO {
    private Integer nombre;
    private List<Integer> permisoIds;
    public Integer getRolId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRolId'");
    }
}
