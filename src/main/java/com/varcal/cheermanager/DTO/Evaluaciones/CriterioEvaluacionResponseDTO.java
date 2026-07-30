package com.varcal.cheermanager.DTO.Evaluaciones;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriterioEvaluacionResponseDTO {
    private Integer id;
    private String nombre;
    private Integer categoriaId;
    private String categoria;
}