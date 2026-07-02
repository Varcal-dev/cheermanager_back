package com.varcal.cheermanager.DTO.Eventos;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ResultadoCompetenciaDTO {
    private Integer eventoId;
    private Integer grupoEntrenamientoId;
    private Integer posicion;
    private BigDecimal puntaje;
    private Integer premioId;           // opcional
    private Integer evaluacionRutinaId; // opcional, si el evento se evaluó dentro del sistema
    private String observaciones;
}