package com.varcal.cheermanager.DTO.Persona;

import java.time.LocalDate;

import lombok.Data;

@Data
public class PersonaDTO {
    private String nombre;
    private String direccion;
    private String telefono;
    private LocalDate fechaNacimiento;
    private Integer generoId;

    
}