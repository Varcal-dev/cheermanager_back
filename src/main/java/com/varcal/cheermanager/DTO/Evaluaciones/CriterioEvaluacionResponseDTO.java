package com.varcal.cheermanager.DTO.Evaluaciones;

import lombok.Data;

@Data
public class CriterioEvaluacionResponseDTO {
    private Integer id;
    private String nombre;
    private Integer categoriaId;
    private String categoria;
}