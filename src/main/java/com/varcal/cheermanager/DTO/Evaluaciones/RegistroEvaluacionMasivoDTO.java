package com.varcal.cheermanager.DTO.Evaluaciones;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class RegistroEvaluacionMasivoDTO {
    private Integer deportistaId;
    private LocalDate fecha;
    private List<RegistroIndividual> registros;

    @Data
    public static class RegistroIndividual {
        private Integer criterioId;
        private Integer puntajeObtenido;
    }
}