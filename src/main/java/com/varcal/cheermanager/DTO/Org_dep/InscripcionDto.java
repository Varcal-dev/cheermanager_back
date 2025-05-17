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
private LocalDate fechaInscripcion;
private LocalDate fechaVencimiento;
private String estado;
}
