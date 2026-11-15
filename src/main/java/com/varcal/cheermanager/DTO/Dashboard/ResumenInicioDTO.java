package com.varcal.cheermanager.DTO.Dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class ResumenInicioDTO {
    private int deportistasActivos;
    private int totalGrupos;

    private ProximoEventoDTO proximoEvento; // null si no hay ninguno agendado

    // Cartera: dato financiero. Solo se llena para quien tiene ver_dash_finanzas o ver_factura; para el resto
    // (entrenador, asistente, inventario...) va null, igual que los permisos de Finanzas que no tienen.
    private BigDecimal carteraPendienteTotal; // suma de (factura.total - pagado) de todas las facturas
    private Long facturasConSaldoPendiente;

    private Double porcentajeAsistenciaUltimos7Dias; // null si no hay registros

    private int alertasCondicionMedicaActiva; // solo conteo, nunca detalle clínico

    @Data
    public static class ProximoEventoDTO {
        private Integer eventoId;
        private String nombre;
        private LocalDate fecha;
        private long diasRestantes;
    }
}