package com.varcal.cheermanager.DTO.Financiero;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

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
    @NotNull(message = "El monto por deportista es obligatorio")
    @Positive(message = "El monto por deportista debe ser mayor a cero")
    private BigDecimal monto;            // obligatorio, > 0: valor FINAL por deportista
    @Size(max = 255, message = "La descripcion admite maximo 255 caracteres")
    private String descripcion;          // opcional: por defecto "Evento: <nombre>"
    private LocalDate fechaVencimiento;  // opcional: por defecto hoy + 10 dias
    private List<Integer> grupoIds;      // opcional: solo estos grupos (deben estar inscritos en el evento)
    private Boolean simular;             // true = vista previa, no crea facturas
}