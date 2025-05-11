package com.varcal.cheermanager.DTO;


import com.varcal.cheermanager.Models.Auth.Usuario;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioDTO {
    private String username;
    private String email;
    private String password;
    private Integer rolId;
    private Integer personaId; // ID de la persona asociada

    // Constructor to initialize UsuarioDTO from a Usuario object
    public UsuarioDTO(Usuario usuario) {
        this.username = usuario.getUsername();
        this.email = usuario.getEmail();
        this.rolId = usuario.getRol() != null ? usuario.getRol().getId() : null;
        // Add other fields as necessary
    }
}