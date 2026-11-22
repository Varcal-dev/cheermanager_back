package com.varcal.cheermanager.DTO.Financiero;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

import lombok.Data;

@Data
public class ConvenioDto {
    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(max = 100, message = "El nombre de la empresa no puede superar 100 caracteres")
    private String nombreEmpresa;
    @NotNull(message = "Selecciona un descuento")
    private Integer descuentoId;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    // getters y setters
}