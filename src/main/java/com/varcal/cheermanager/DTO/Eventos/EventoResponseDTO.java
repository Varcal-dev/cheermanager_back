package com.varcal.cheermanager.DTO.Eventos;

import java.time.LocalDate;

import lombok.Data;

@Data
public class EventoResponseDTO {
    private Integer id;
    private String nombre;
    private LocalDate fecha;
    private Integer tipoEventoId;
    private String tipoEvento;
    private String ubicacion;
    private Boolean tieneResultados;
    private Integer cantidadGruposInscritos;
    private Integer cantidadPremios;
}