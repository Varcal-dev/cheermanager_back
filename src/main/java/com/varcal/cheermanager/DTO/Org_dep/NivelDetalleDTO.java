package com.varcal.cheermanager.DTO.Org_dep;

import java.util.List;

import lombok.Data;

@Data
public class NivelDetalleDTO {
    private Integer id;
    private String nombre;
    private String descripcion;

    private List<CategoriaNivelDTO> categorias;
}
