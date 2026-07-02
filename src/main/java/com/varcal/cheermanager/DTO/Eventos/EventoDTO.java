package com.varcal.cheermanager.DTO.Eventos;

import java.time.LocalDate;

import lombok.Data;

@Data
public class EventoDTO {
    private String nombre;
    private LocalDate fecha;
    private Integer tipoEventoId;
    private String ubicacion;
    private Boolean tieneResultados; // si el productor del evento publica resultados oficiales
}