package com.varcal.cheermanager.DTO.Persona;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class HistorialDeportistaEstadoDTO {
    private Integer id;
    private Integer deportistaId;
    private String nombreDeportista;
    private String apellidosDeportista;
    private Integer estadoAnteriorId;
    private String estadoAnteriorNombre;
    private Integer estadoNuevoId;
    private String estadoNuevoNombre;
    private LocalDateTime fechaCambio;
    private Integer usuarioId;
    private String usuarioUsername;
    private String motivoCambio;
}
