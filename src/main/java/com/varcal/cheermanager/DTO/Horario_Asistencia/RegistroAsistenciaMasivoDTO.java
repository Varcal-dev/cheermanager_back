package com.varcal.cheermanager.DTO.Horario_Asistencia;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class RegistroAsistenciaMasivoDTO {
    @NotNull(message = "Indica el horario")
    private Integer horarioId;
    @NotNull(message = "Indica la fecha del entrenamiento")
    private LocalDate fecha;
    @NotNull(message = "Indica al menos un registro")
    private List<RegistroIndividual> registros;

    @Data
    public static class RegistroIndividual {
        private Integer deportistaId;
        private Integer estadoAsistenciaId;
    }
}