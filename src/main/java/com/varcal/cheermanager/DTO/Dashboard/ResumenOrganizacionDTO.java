package com.varcal.cheermanager.DTO.Dashboard;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class ResumenOrganizacionDTO {
    private List<GrupoOcupacionDTO> gruposConOcupacion;
    private List<GrupoOcupacionDTO> gruposPorDebajoDelMinimo; // subconjunto del anterior, para alertar directo
    private Map<String, Long> gruposPorNivel; // ej. {"Elite 1": 3, "Novice N1": 2, ...}

    @Data
    public static class GrupoOcupacionDTO {
        private Integer grupoId;
        private String nombreGrupo;
        private String nivel;
        private String division;
        private long deportistasActivos;
        private long hombres;
        private long mujeres;
        private Integer cantidadMinRequerida; // null si la categoría no tiene regla cargada
        private Integer cantidadMaxSugerida;
        private Boolean mixto; // true si hay al menos 1 hombre y 1 mujer
    }
}