package com.varcal.cheermanager.DTO.Financiero;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

/**
 * Cobro manual fuera de la mensualidad (uniforme, competencia, evento, ...).
 * El numero de factura se genera solo; el total es el valor FINAL a cobrar.
 */
@Data
public class FacturaManualDTO {
    private Integer personaId;          // obligatorio: a quien se le cobra
    private String concepto;            // UNIFORME | COMPETENCIA | EVENTO | OTRO
    private String descripcion;         // obligatorio
    private BigDecimal total;           // obligatorio, > 0
    private LocalDate fechaEmision;     // opcional: por defecto hoy
    private LocalDate fechaVencimiento; // opcional: por defecto emision + 10 dias
    private Integer eventoId;           // opcional: liga el cobro a un evento
}
