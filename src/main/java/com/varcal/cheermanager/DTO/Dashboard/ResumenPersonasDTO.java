package com.varcal.cheermanager.DTO.Dashboard;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class ResumenPersonasDTO {

    // --- Deportistas ---
    private int totalDeportistas;
    private Map<String, Long> deportistasPorEstado;    // ej. {"Activo": 42, "Inactivo": 5}
    private Map<String, Long> deportistasPorGenero;     // ej. {"Femenino": 38, "Masculino": 9}
    private Map<String, Long> deportistasPorDivision;   // división FEDECOLCHEER real (Tiny/Mini/Youth/...), no rango de edad inventado
    private int deportistasConCondicionMedicaActiva;    // solo conteo, nunca detalle clínico acá

    // --- Altas y bajas (HistorialDeportistaEstado) ---
    private long altasRecientes;
    private long bajasRecientes;
    private List<MovimientoEstadoDTO> movimientosRecientes;

    // --- Entrenadores ---
    private int totalEntrenadores;
    private List<EntrenadorResumenDTO> entrenadores;

    @Data
    public static class EntrenadorResumenDTO {
        private Integer entrenadorId;
        private String nombreCompleto;
        private String especializacion;
        private String estado;
        private long gruposACargo;
    }

    @Data
    public static class MovimientoEstadoDTO {
        private Integer deportistaId;
        private String nombreCompleto;
        private String estadoAnterior; // null si es el primer registro del deportista
        private String estadoNuevo;
        private LocalDateTime fechaCambio;
        private Boolean esAlta; // true si estadoNuevo = "Activo"
        private String motivo;
    }
}