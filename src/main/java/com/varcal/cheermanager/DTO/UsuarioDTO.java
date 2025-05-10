package com.varcal.cheermanager.DTO;

import com.varcal.cheermanager.Models.Auth.Usuario;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UsuarioDTO {
    private Long id;
    private String username;
    private String email;
    private LocalDateTime ultimoAcceso;
    private Boolean activo;

    // Constructor para mapear desde el modelo Usuario
    public UsuarioDTO(Usuario user) {
        this.id = Long.valueOf(user.getId());
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.ultimoAcceso = user.getUltimoAcceso();
        this.activo = user.getActivo();
    }

}