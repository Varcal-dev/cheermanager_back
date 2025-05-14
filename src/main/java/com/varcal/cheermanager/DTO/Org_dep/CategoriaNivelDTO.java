package com.varcal.cheermanager.DTO.Org_dep;

import lombok.Data;

@Data
public class CategoriaNivelDTO {
    private Integer id;
    private String nombre;
    private String division;
    private Integer añoAplicacion;
    private Integer añoNacimientoMin;
    private Integer añoNacimientoMax;
    private Integer cantidadMin;
    private Integer cantidadMax;
    private String restricciones;
}
