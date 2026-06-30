package com.varcal.cheermanager.DTO.Evaluaciones;

import java.time.LocalDate;

import lombok.Data;

@Data
public class ObjetivoResponseDTO {
    private Integer id;
    private Integer deportistaId;
    private String nombreDeportista;
    private String nombre;
    private String descripcion;
    private LocalDate fechaCreacion;
    private Integer estadoObjetivoId;
    private String estadoObjetivo;
}