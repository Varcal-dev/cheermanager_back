package com.varcal.cheermanager.DTO.Historiales;

import java.time.LocalDate;

import lombok.Data;

@Data
public class HistorialPlanesDeportistaDTO {
    private Integer id;
    private Integer deportistaId;
    private String nombreDeportista;
    private Integer planPagoId;
    private String nombrePlan;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String motivoCambio;
}