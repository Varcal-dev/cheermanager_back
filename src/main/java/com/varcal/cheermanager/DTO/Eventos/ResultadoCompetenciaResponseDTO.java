package com.varcal.cheermanager.DTO.Eventos;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class ResultadoCompetenciaResponseDTO {
    private Integer id;
    private Integer eventoId;
    private String nombreEvento;
    private LocalDate fechaEvento;
    private Integer grupoEntrenamientoId;
    private String nombreGrupo;
    private Integer posicion;
    private BigDecimal puntaje;
    private Integer premioId;
    private String descripcionPremio;
    private Integer evaluacionRutinaId;
    private String observaciones;
}