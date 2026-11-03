package com.varcal.cheermanager.DTO.Evaluaciones;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

/** Un criterio evaluado a varios deportistas (p. ej. a todo un grupo) el mismo dia. */
@Data
public class EvaluacionPorCriterioDTO {
    private Integer criterioId;
    private LocalDate fecha;
    private List<Registro> registros;

    @Data
    public static class Registro {
        private Integer deportistaId;
        private Integer puntajeObtenido;
    }
}
