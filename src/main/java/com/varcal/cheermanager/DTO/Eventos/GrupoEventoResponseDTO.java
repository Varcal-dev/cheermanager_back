package com.varcal.cheermanager.DTO.Eventos;

import java.time.LocalDate;

import lombok.Data;

@Data
public class GrupoEventoResponseDTO {
    private Integer id;
    private Integer grupoEntrenamientoId;
    private String nombreGrupo;
    private Integer eventoId;
    private String nombreEvento;
    private LocalDate fechaEvento;
}