package com.varcal.cheermanager.DTO.Financiero;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

/**
 * Cobro manual fuera de la mensualidad (uniforme, competencia, evento, ...).
 * El numero de factura se genera solo; el total es el valor FINAL a cobrar.
 */
@Data
public class FacturaManualDTO {
    @NotNull(message = "Indica a quien se le cobra")
    private Integer personaId;          // obligatorio: a quien se le cobra
    @Size(max = 30, message = "El concepto admite maximo 30 caracteres")
    private String concepto;            // UNIFORME | COMPETENCIA | EVENTO | OTRO
    @NotBlank(message = "La descripcion del cobro es obligatoria")
    @Size(max = 255, message = "La descripcion admite maximo 255 caracteres")
    private String descripcion;         // obligatorio
    @NotNull(message = "El total es obligatorio")
    @Positive(message = "El total debe ser mayor a cero")
    private BigDecimal total;           // obligatorio, > 0
    private LocalDate fechaEmision;     // opcional: por defecto hoy
    private LocalDate fechaVencimiento; // opcional: por defecto emision + 10 dias
    private Integer eventoId;           // opcional: liga el cobro a un evento
}
