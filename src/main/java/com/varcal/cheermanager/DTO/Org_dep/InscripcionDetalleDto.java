package com.varcal.cheermanager.DTO.Org_dep;

import java.time.LocalDate;

import lombok.Data;

@Data
public class InscripcionDetalleDto {
    private Long inscripcionId;
    private Long deportistaId;
    private String deportista;
    private String nivel;
    private LocalDate fechaInscripcion;
    private LocalDate fechaVencimiento;
    private String estado;
    private String planPago;
    private String grupoAsignado;
    private Boolean documentosCompletos;

    // Getters y setters
}
