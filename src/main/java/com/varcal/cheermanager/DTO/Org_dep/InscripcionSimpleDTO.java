package com.varcal.cheermanager.DTO.Org_dep;

import java.time.LocalDate;

import lombok.Data;

@Data
public class InscripcionSimpleDTO {
    private Integer id;
    private String deportista;
    private LocalDate fechaInscripcion;
    private String planPago;
    private String estado;
}
