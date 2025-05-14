package com.varcal.cheermanager.DTO.Persona;

import java.time.LocalDate;

import lombok.Data;

@Data
public class DeportistaDTO {
    private Integer id;
    private String nombre;
    private String apellidos;
    private String direccion;
    private String telefono;
    private LocalDate fechaNacimiento;
    private Integer generoId;
    private Integer estadoId;
    private Integer altura;
    private Integer peso;
    private Integer nivelActualId;
    private LocalDate fechaInscripcion;
    private String contactoEmergencia;
    private Integer convenioId;    
}
