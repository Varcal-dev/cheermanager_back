package com.varcal.cheermanager.DTO.Persona;

import java.time.LocalDate;

import com.varcal.cheermanager.Models.Personas.Persona;

import lombok.Data;

@Data
public class DeportistaListadoDTO {
    private Integer id;
    private Persona persona; // Información de la persona asociada
    private String estado; // Nombre del estado
    private String nivel; // Nombre del nivel actual
    private LocalDate fechaInscripcion;
    private String contactoEmergencia;
    private String convenio; // Nombre del convenio
}
