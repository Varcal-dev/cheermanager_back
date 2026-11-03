package com.varcal.cheermanager.DTO.Evaluaciones;

import lombok.Data;

@Data
public class PromedioCriterioDTO {
    private Integer criterioId;
    private String criterio;
    private String categoria;
    private double promedio;
    private long evaluaciones;
    private long deportistas;
}
