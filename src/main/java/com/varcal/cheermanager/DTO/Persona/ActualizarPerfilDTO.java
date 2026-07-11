package com.varcal.cheermanager.DTO.Persona;

import java.time.LocalDate;

import lombok.Data;

// DTO deliberadamente reducido para el endpoint de auto-servicio
// (PUT /api/usuario/me): solo los campos que un usuario puede tocar de
// SU PROPIO perfil. A diferencia de UsuarioDTO, no incluye password,
// rolId ni personaId — esos siguen siendo exclusivos del endpoint
// administrativo (PUT /api/usuario/actualizar/{id}, "modificar_usuario").
@Data
public class ActualizarPerfilDTO {
    private String email;
    private String nombre;
    private String direccion;
    private String telefono;
    private LocalDate fechaNacimiento;
    private Integer generoId;
}