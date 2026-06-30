package com.varcal.cheermanager.DTO.Funcionalidad;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class LogAccesoDTO {
    private Integer id;
    private Integer usuarioId;
    private String username;
    private String emailIntento;
    private LocalDateTime fecha;
    private String accion;
    private String ipOrigen;
    private String detalle;
}