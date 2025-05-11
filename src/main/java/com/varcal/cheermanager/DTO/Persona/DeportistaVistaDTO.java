package com.varcal.cheermanager.DTO.Persona;

import java.time.LocalDate;

import lombok.Data;

@Data
public class DeportistaVistaDTO {
    private Integer deportistaId;
    private Integer personaId;
    private String nombre;
    private String apellidos;
    private String direccion;
    private String telefono;
    private LocalDate fechaNacimiento;
    private Integer generoId;
    private String genero;
    private Float altura; // Nuevo campo
    private Float peso;   // Nuevo campo
    private LocalDate fechaInscripcion;
    private String contactoEmergencia;
    private Integer estadoId;
    private String estadoNombre;
    private Integer nivelActualId;
    private String nivelNombre;
    private Integer convenioId;
    private String convenioNombre;
}
