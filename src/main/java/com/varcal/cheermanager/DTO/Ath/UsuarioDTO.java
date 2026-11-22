package com.varcal.cheermanager.DTO.Ath;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import com.varcal.cheermanager.DTO.Comunes.Crear;
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
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 255, message = "El nombre de usuario no puede superar 255 caracteres")
    private String username;
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(groups = Crear.class, message = "El correo electrónico no tiene un formato válido")
    @Size(max = 255, message = "El correo no puede superar 255 caracteres")
    private String email;
    // Al crear es obligatoria; al editar es opcional (vacía = no cambiar) y el controlador valida el mínimo.
    @NotBlank(groups = Crear.class, message = "La contraseña es obligatoria")
    @Size(groups = Crear.class, min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    private String password;
    private Integer rolId;
    private String rolNombre;
    private LocalDateTime ultimoAcceso;
    private Boolean activo;
    private Boolean requiereCambioPassword;
    private Integer personaId;

    // Datos de la persona
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    private String nombre;
    @Size(max = 255, message = "Los apellidos no puede superar 255 caracteres")
    private String apellidos;
    @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
    private String direccion;
    @Size(max = 255, message = "El teléfono no puede superar 255 caracteres")
    private String telefono;
    @PastOrPresent(message = "La fecha de nacimiento no puede ser futura")
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
        this.requiereCambioPassword = usuario.getRequiereCambioPassword();
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