package com.varcal.cheermanager.DTO.Org_dep;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrupoEntrenamientoResponseDTO {
     private Long id;
    private String nombre;
    private String descripcion;
    private Long nivelId;
    private String nombreNivel;
    private Long temporadaId;
    private String nombreTemporada;
    private Integer cantidadDeportistas;

}
