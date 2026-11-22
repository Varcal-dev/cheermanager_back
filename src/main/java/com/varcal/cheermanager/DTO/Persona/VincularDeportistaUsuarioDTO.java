package com.varcal.cheermanager.DTO.Persona;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VincularDeportistaUsuarioDTO {
    @NotNull(message = "El usuario es obligatorio")
    private Integer usuarioId;
    private Integer deportistaId;
}