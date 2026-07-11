package com.varcal.cheermanager.DTO.Dashboard;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class ResumenAdministracionDTO {

    // --- Salud financiera (resumen, el detalle completo vive en /dashboard/finanzas) ---
    private BigDecimal recaudoMesActual;
    private BigDecimal carteraPendienteTotal;

    // --- Salud operativa ---
    private Double porcentajeAsistenciaGeneral30Dias;

    // --- Salud competitiva ---
    private Double puntajePromedioClub; // promedio del puntaje más reciente de cada grupo con evaluaciones
    private int gruposConEvaluacionReciente;

    // --- Uso del sistema ---
    private long loginsExitosos7Dias;
    private long loginsFallidos7Dias;
    private long usuariosActivos7Dias; // distintos que tuvieron al menos un login exitoso

    // --- Auditoría reciente ---
    private List<RegistroAuditoriaDTO> auditoriaReciente;

    @Data
    public static class RegistroAuditoriaDTO {
        private String usuario;
        private String tablaAfectada;
        private String accion;
        private Integer registroId;
        private LocalDateTime fecha;
    }
}