package com.varcal.cheermanager.DTO.Dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class ResumenEvaluacionDTO {
    private List<UltimoPuntajeGrupoDTO> ultimosPuntajesPorGrupo;

    @Data
    public static class UltimoPuntajeGrupoDTO {
        private Integer grupoId;
        private String nombreGrupo;
        private String nivel;
        private LocalDate fecha;
        private String evento; // texto libre (puede ser simulacro/práctica)
        private BigDecimal puntajeTotal;
    }

    // Respuesta separada para GET /api/dashboard/evaluacion/progresion/{grupoId}
    @Data
    public static class ProgresionGrupoDTO {
        private Integer grupoId;
        private String nombreGrupo;
        private List<PuntoProgresionDTO> puntos; // ordenados de más antiguo a más reciente

        @Data
        public static class PuntoProgresionDTO {
            private LocalDate fecha;
            private String evento;
            private Boolean esEventoOficial; // true si eventoOficial (FK) está diligenciado
            private BigDecimal puntajeTotal;
        }
    }
}