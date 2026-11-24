package com.varcal.cheermanager.DTO.Org_dep;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

import lombok.Data;

@Data
public class InscripcionDto {
private Integer id;
@NotNull(message = "El deportista es obligatorio")
private Integer deportistaId;
private String nombreDeportista; 
@NotNull(message = "El plan de pago es obligatorio")
private Integer planPagoId;
private String nombrePlan;

// Nuevo: plan de mensualidad opcional. Si viene con valor, al crear la
// inscripción se genera también la factura del primer mes de mensualidad
// (ver InscripcionService.crearInscripcion).
private Integer planMensualidadId;

@NotNull(message = "La fecha de inscripción es obligatoria")
private LocalDate fechaInscripcion;
private LocalDate fechaVencimiento;
@NotBlank(message = "El estado es obligatorio")
private String estado;

// Opcional: temporada a la que pertenece la inscripcion (renovaciones/solicitudes). Sin valor = temporada activa.
private Integer temporadaId;
}