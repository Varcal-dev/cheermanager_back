package com.varcal.cheermanager.DTO.Funcionalidad;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AuditoriaDTO {
    private Integer id;
    private Integer usuarioId;
    private String usuarioUsername;
    private String tablaAfectada;
    private String accion; // INSERT | UPDATE | DELETE
    private Integer registroId;
    private String valoresAnteriores; // JSON, null en INSERT
    private String nuevosValores; // JSON, null en DELETE
    private LocalDateTime fecha;
    private String ipUsuario;
}