package com.varcal.cheermanager.DTO.Eventos;

import lombok.Data;

@Data
public class TipoEventoDTO {
    private String evento; // nombre del tipo, ej. "Competencia federada", "Simulacro", "Clínica"
}