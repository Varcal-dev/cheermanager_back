package com.varcal.cheermanager.DTO.Financiero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;

/**
 * Cobro de un evento a todos los deportistas de los grupos que participan.
 * Con simular=true no crea nada: solo devuelve a quien se le cobraria.
 */
@Data
public class CobroEventoLoteDTO {
    private BigDecimal monto;            // obligatorio, > 0: valor FINAL por deportista
    private String descripcion;          // opcional: por defecto "Evento: <nombre>"
    private LocalDate fechaVencimiento;  // opcional: por defecto hoy + 10 dias
    private List<Integer> grupoIds;      // opcional: solo estos grupos (deben estar inscritos en el evento)
    private Boolean simular;             // true = vista previa, no crea facturas
}