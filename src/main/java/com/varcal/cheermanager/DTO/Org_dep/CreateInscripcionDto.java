package com.varcal.cheermanager.DTO.Org_dep;

import java.time.LocalDate;

import lombok.Data;

@Data
public class CreateInscripcionDto {
private Integer deportistaId;
private Integer planPagoId;
private LocalDate fechaInscripcion;
private LocalDate fechaVencimiento;
private String estado; // Activa o Inactiva
}

