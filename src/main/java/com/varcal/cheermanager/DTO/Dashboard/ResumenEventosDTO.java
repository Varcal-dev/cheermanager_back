package com.varcal.cheermanager.DTO.Dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class ResumenEventosDTO {
    private List<ProximoEventoResumenDTO> proximosEventos;
    private long podiosTemporada; // conteo de resultados con posicion <= 3, todos los eventos con fecha pasada
    private List<PremioGanadoDTO> premiosGanados;

    @Data
    public static class ProximoEventoResumenDTO {
        private Integer eventoId;
        private String nombre;
        private LocalDate fecha;
        private long gruposInscritos;
    }

    @Data
    public static class PremioGanadoDTO {
        private Integer eventoId;
        private String nombreEvento;
        private LocalDate fechaEvento;
        private String nombreGrupo;
        private Integer posicion;
        private BigDecimal puntaje;
        private String descripcionPremio;
    }
}