package com.varcal.cheermanager.DTO.Persona;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

import lombok.Data;

// DTO deliberadamente reducido para el endpoint de auto-servicio
// (PUT /api/usuario/me): solo los campos que un usuario puede tocar de
// SU PROPIO perfil. A diferencia de UsuarioDTO, no incluye password,
// rolId ni personaId — esos siguen siendo exclusivos del endpoint
// administrativo (PUT /api/usuario/actualizar/{id}, "modificar_usuario").
@Data
public class ActualizarPerfilDTO {
    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 255, message = "El correo no puede superar 255 caracteres")
    private String email;
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    private String nombre;
    @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
    private String direccion;
    @Size(max = 255, message = "El teléfono no puede superar 255 caracteres")
    private String telefono;
    @PastOrPresent(message = "La fecha de nacimiento no puede ser futura")
    private LocalDate fechaNacimiento;
    private Integer generoId;
}