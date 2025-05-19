package com.varcal.cheermanager.DTO.Persona;

import java.time.LocalDate;

import lombok.Data;

@Data
public class EntrenadorDTO {
    private Integer rolIdE;
    private String nombre;
    private String apellidos;
    private String direccion;
    private String telefono;
    private LocalDate fechaNacimiento;
    private Integer generoId;
    private String especializacion;
    private LocalDate fechaContratacion;
    private Integer estadoId;

    
}
