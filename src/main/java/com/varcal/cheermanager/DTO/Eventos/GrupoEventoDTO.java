package com.varcal.cheermanager.DTO.Eventos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GrupoEventoDTO {
    @NotNull(message = "El grupo es obligatorio")
    private Integer grupoEntrenamientoId;
    @NotNull(message = "El evento es obligatorio")
    private Integer eventoId;
}