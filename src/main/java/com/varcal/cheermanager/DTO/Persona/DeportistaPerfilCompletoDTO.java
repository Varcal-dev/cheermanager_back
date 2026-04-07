package com.varcal.cheermanager.DTO.Persona;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class DeportistaPerfilCompletoDTO {

    // Información Personal
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

    // Información Deportiva
    private Float altura;
    private Float peso;
    private LocalDate fechaRegistro;
    private Integer estadoId;
    private String estadoNombre;
    private Integer nivelActualId;
    private String nivelNombre;
    private Integer convenioId;
    private String convenioNombre;

    // Información de Contacto de Emergencia
    private String contactoEmergencia;

    // Información del Usuario del Sistema (vinculado)
    private Integer usuarioId;
    private String username;
    private String email;
    private Boolean usuarioActivo;
    private LocalDateTime ultimoAcceso;
    private List<String> roles;
    private List<String> permisos;
}
