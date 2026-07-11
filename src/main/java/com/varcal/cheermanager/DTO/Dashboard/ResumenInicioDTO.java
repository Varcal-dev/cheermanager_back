package com.varcal.cheermanager.DTO.Dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class ResumenInicioDTO {
    private int deportistasActivos;
    private int totalGrupos;

    private ProximoEventoDTO proximoEvento; // null si no hay ninguno agendado

    private BigDecimal carteraPendienteTotal; // suma de (factura.total - pagado) de todas las facturas
    private long facturasConSaldoPendiente;

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