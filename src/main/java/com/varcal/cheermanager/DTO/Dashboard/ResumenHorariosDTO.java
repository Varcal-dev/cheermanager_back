package com.varcal.cheermanager.DTO.Dashboard;

import java.util.List;

import lombok.Data;

@Data
public class ResumenHorariosDTO {
    private int diasAnalizados; // rango usado para el cálculo, ej. 30
    private Double porcentajeAsistenciaGeneral; // promedio simple entre grupos con datos

    private List<AsistenciaPorGrupoDTO> asistenciaPorGrupo;
    private List<DeportistaEnRiesgoDTO> deportistasEnRiesgo; // < 70% de asistencia

    @Data
    public static class AsistenciaPorGrupoDTO {
        private Integer grupoId;
        private String nombreGrupo;
        private long totalRegistros;
        private long presentes;
        private Double porcentaje; // null si no hay registros en el rango
    }

    @Data
    public static class DeportistaEnRiesgoDTO {
        private Integer deportistaId;
        private String nombreCompleto;
        private Double porcentaje;
    }
}