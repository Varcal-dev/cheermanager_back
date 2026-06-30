package com.varcal.cheermanager.DTO.Historiales;

import java.time.LocalDate;

import lombok.Data;

@Data
public class HistorialNivelDeportistaDTO {
    private Integer id;
    private Integer deportistaId;
    private String nombreDeportista;
    private Integer nivelId;
    private LocalDate fechaCambio;
    private String motivo;
}