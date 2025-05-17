package com.varcal.cheermanager.DTO.Financiero;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class PlanPagoDTO {
    private Integer tipoPlan; // solo el ID
    private String descripcion;
    private BigDecimal valorMensual;
    private LocalDate fechaVigenciaInicio;
    private LocalDate fechaVigenciaFin;
    private Boolean activo;

    // getters y setters
}
