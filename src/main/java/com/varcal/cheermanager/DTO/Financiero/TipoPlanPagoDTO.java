package com.varcal.cheermanager.DTO.Financiero;

import lombok.Data;

@Data
public class TipoPlanPagoDTO {
    private Integer id;
    private String nombre;
    private String categoria; // "inscripcion" | "mensualidad"
    private Integer duracionMeses;
    private String descripcion;
    private Long planesAsociados; // solo lectura, informativo en la tabla
}