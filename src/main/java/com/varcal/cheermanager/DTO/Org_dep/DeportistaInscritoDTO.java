package com.varcal.cheermanager.DTO.Org_dep;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class DeportistaInscritoDTO {
    private Integer deportistaId;
    private String nombre;
    private String apellidos;
    private String telefono;
    private LocalDate fechaNacimiento;
    private String genero;
    private String nivel;
    private LocalDate fechaInscripcion;
    private LocalDate fechaVencimiento;
    private String estadoInscripcion;
    private String planPago;
    private String tipoPlan;
    private BigDecimal valorMensual;
    private String estadoDeportista;
    private String grupoEntrenamiento;
}