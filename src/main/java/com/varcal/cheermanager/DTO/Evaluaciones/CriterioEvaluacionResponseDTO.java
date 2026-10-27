package com.varcal.cheermanager.DTO.Evaluaciones;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CriterioEvaluacionResponseDTO {
    private Integer id;
    private String nombre;
    private Integer categoriaId;
    private String categoria;
    /** true si ya tiene evaluaciones registradas (no se puede borrar ni mover de categoria). */
    private boolean enUso;
}