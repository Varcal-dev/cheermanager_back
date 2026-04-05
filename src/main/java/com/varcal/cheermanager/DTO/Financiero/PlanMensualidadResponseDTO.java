package com.varcal.cheermanager.DTO.Financiero;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class PlanMensualidadResponseDTO {
    private Integer id;
    private String nombre;
    private String tipoPlan;
    private String descripcion;
    private BigDecimal valorMensual;
    private BigDecimal descuentoPorcentaje;
    private Integer sesionesSemanales;
    private Boolean activo;
    private LocalDate fechaVigenciaInicio;
    private LocalDate fechaVigenciaFin;
}
