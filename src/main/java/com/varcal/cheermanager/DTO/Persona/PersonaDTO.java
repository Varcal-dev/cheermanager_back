package com.varcal.cheermanager.DTO.Persona;

import java.time.LocalDate;

import lombok.Data;

@Data
public class PersonaDTO {
    @jakarta.validation.constraints.NotBlank(message = "El nombre es obligatorio")
    @jakarta.validation.constraints.Size(max = 255, message = "El nombre admite maximo 255 caracteres")
    private String nombre;
    @jakarta.validation.constraints.Size(max = 255, message = "La direccion admite maximo 255 caracteres")
    private String direccion;
    @jakarta.validation.constraints.Size(max = 255, message = "El telefono admite maximo 255 caracteres")
    private String telefono;
    private LocalDate fechaNacimiento;
    private Integer generoId;

    
}