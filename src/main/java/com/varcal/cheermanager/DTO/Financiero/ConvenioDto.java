package com.varcal.cheermanager.DTO.Financiero;

import java.time.LocalDate;

import lombok.Data;

@Data
public class ConvenioDto {
    private String nombreEmpresa;
    private Integer descuentoId;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    // getters y setters
}
