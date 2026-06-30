package com.varcal.cheermanager.DTO.Evaluaciones.Rutina;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class EvaluacionRutinaResponseDTO {
    private Integer id;
    private Integer grupoId;
    private String nombreGrupo;
    private Integer nivelId;
    private String nivel;
    private LocalDate fecha;
    private String evento;
    private Integer cantidadAtletas;
    private BigDecimal puntajeTotal;
    private String observaciones;
    private List<RegistroSubCriterioResponseDTO> registros;

    @Data
    public static class RegistroSubCriterioResponseDTO {
        private Integer id;
        private Integer subCriterioId;
        private String nombreSubCriterio;
        private String seccion;
        private BigDecimal puntajeBase;
        private BigDecimal puntajeDrivers;
        private BigDecimal puntajeFinal;
        // Texto explicando qué escalón de la rúbrica se aplicó, para
        // trazabilidad (ej. "GRAN PARTE realizó 3 habilidades diferentes -> 4.0")
        private String detalleCalculo;
    }
}