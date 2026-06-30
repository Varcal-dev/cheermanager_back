package com.varcal.cheermanager.DTO.Horario_Asistencia;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class RegistroAsistenciaMasivoDTO {
    private Integer horarioId;
    private LocalDate fecha;
    private List<RegistroIndividual> registros;

    @Data
    public static class RegistroIndividual {
        private Integer deportistaId;
        private Integer estadoAsistenciaId;
    }
}