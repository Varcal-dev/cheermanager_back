package com.varcal.cheermanager.DTO.Evaluaciones;

import java.time.LocalDate;

import lombok.Data;

@Data
public class ObjetivoDTO {
    private Integer deportistaId;
    private String nombre;
    private String descripcion;
    private LocalDate fechaCreacion;
    private Integer estadoObjetivoId;
}