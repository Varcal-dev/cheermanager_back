package com.varcal.cheermanager.DTO.Org_dep;

import java.util.List;

import com.varcal.cheermanager.DTO.Persona.DeportistaDTO;
import com.varcal.cheermanager.Models.Org_dep.CategoriaNivel;
import com.varcal.cheermanager.Models.Org_dep.TipoGrupo;

import lombok.Data;

@Data
public class GrupoConDeportistasDTO {
    private Integer id;
    private String nombre;
    private TipoGrupo tipoGrupo;
    private CategoriaNivel categoriaNivel;
    private List<DeportistaDTO> deportistas;
}
