package com.varcal.cheermanager.DTO.Ath;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Personas.Persona;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioDTO {
    private Integer id;
    private String username;
    private String email;
    private String password;
    private Integer rolId;
    private String rolNombre;
    private LocalDateTime ultimoAcceso;
    private Boolean activo;
    private Integer personaId;

    // Datos de la persona
    private String nombre;
    private String apellidos;
    private String direccion;
    private String telefono;
    private LocalDate fechaNacimiento;
    private Integer generoId;

    // Constructor para inicializar desde un Usuario
    public UsuarioDTO(Usuario usuario) {
        this.id = usuario.getId();
        this.username = usuario.getUsername();
        this.email = usuario.getEmail();
        this.rolId = usuario.getRol() != null ? usuario.getRol().getId() : null;
        this.rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : null; // Asignar el nombre del rol
        this.ultimoAcceso = usuario.getUltimoAcceso();
        this.activo = usuario.getActivo();
        // Mapear los datos de la persona asociada
        Persona persona = usuario.getPersona();
        if (persona != null) {
            this.personaId = persona.getId();
            this.nombre = persona.getNombre();
            this.apellidos = persona.getApellidos();
            this.direccion = persona.getDireccion();
            this.telefono = persona.getTelefono();
            this.fechaNacimiento = persona.getFechaNacimiento();
            this.generoId = persona.getGeneroId();
        }
    }
}