package com.varcal.cheermanager.DTO.Eventos;

import lombok.Data;

@Data
public class PremioEventoResponseDTO {
    private Integer id;
    private Integer eventoId;
    private String nombreEvento;
    private String descripcion;
    private String premio;
}