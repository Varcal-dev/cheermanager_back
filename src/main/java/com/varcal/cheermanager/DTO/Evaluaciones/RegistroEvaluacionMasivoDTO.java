package com.varcal.cheermanager.DTO.Evaluaciones;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class RegistroEvaluacionMasivoDTO {
    @NotNull(message = "Indica el deportista")
    private Integer deportistaId;
    private LocalDate fecha;
    private List<RegistroIndividual> registros;

    @Data
    public static class RegistroIndividual {
        private Integer criterioId;
        private Integer puntajeObtenido;
    }
}