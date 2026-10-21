package com.varcal.cheermanager.DTO.Org_dep;

import java.time.LocalDate;

import lombok.Data;

@Data
public class InscripcionDto {
private Integer id;
private Integer deportistaId;
private String nombreDeportista; 
private Integer planPagoId;
private String nombrePlan;

// Nuevo: plan de mensualidad opcional. Si viene con valor, al crear la
// inscripción se genera también la factura del primer mes de mensualidad
// (ver InscripcionService.crearInscripcion).
private Integer planMensualidadId;

private LocalDate fechaInscripcion;
private LocalDate fechaVencimiento;
private String estado;

// Opcional: temporada a la que pertenece la inscripcion (renovaciones/solicitudes). Sin valor = temporada activa.
private Integer temporadaId;
}