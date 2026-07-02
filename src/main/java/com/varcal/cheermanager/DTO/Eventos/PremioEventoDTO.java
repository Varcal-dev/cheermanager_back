package com.varcal.cheermanager.DTO.Eventos;

import lombok.Data;

@Data
public class PremioEventoDTO {
    private Integer eventoId;
    private String descripcion; // ej. "1er lugar Nivel 3 Youth"
    private String premio;      // ej. "Trofeo + beca 20% próxima temporada"
}