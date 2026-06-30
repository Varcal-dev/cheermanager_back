package com.varcal.cheermanager.DTO.Evaluaciones.Rutina;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class EvaluacionRutinaDTO {
    private Integer grupoId;
    private Integer nivelId;
    private LocalDate fecha;
    private String evento;
    private Integer cantidadAtletas;
    private String observaciones;
    private List<RegistroSubCriterioDTO> registros;

    @Data
    public static class RegistroSubCriterioDTO {
        private Integer subCriterioId;

        // Para tipos ESCALON_*
        private Integer habilidadesDiferentes;
        private String grupoMinimoAlcanzado; // MAYORIA | GRAN_PARTE | MAXIMO

        // Para tipo RANGO_DIRECTO_JUEZ
        private BigDecimal puntajeFinalManual;

        // Para tipo VALOR_INICIAL_MENOS_DRIVERS
        private List<DeduccionDriverDTO> deducciones;

        // Para Construcciones: Grado de Dificultad + Participación Máxima
        private List<DriverConstruccionDTO> driversConstruccion;
    }

    @Data
    public static class DeduccionDriverDTO {
        private String nombreDriver;
        private Integer nivelDeduccion; // 0,1,2,3
    }

    @Data
    public static class DriverConstruccionDTO {
        private Integer driverId;
        private Integer numeroHabilidad; // para Grado de Dificultad
        private Boolean esNivelAlto;     // para Grado de Dificultad
        private Integer escalonSeleccionadoId; // para Participación Máxima
    }
}