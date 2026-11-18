package com.varcal.cheermanager.DTO.Persona;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class DeportistaVistaDTO {
    private Integer deportistaId;
    private Integer personaId;
    private String nombre;
    private String apellidos;
    private String numeroDocumento;
    private String tipoDocumento;
    private String direccion;
    private String telefono;
    private LocalDate fechaNacimiento;
    private Integer generoId;
    private String genero;
    private Float altura;
    private Float peso;
    private LocalDate fechaRegistro;
    private String contactoEmergencia;
    private Integer estadoId;
    private String estadoNombre;
    private Integer nivelActualId;
    private String nivelNombre;
    private Integer convenioId;
    private String convenioNombre;
    private Boolean tieneCondicionActiva;
    /** Nombres de los grupos de entrenamiento en que esta hoy (membresias vigentes). */
    private List<String> grupos = new ArrayList<>();
    /** false = no tiene ningun acudiente vinculado. */
    private Boolean tieneAcudiente;
}