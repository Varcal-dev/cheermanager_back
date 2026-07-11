package com.varcal.cheermanager.DTO.Dashboard;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class ResumenFinanzasDTO {
    private BigDecimal recaudoMesActual;
    private BigDecimal facturadoMesActual; // referencia de comparación ("proyectado" ~ lo que se facturó este mes)

    private BigDecimal carteraPendienteTotal;
    private long facturasConSaldoPendiente;

    private List<MorosidadPorPlanDTO> morosidadPorPlan;

    private BigDecimal ingresoTiendaMesActual; // ventas de Inventario del mes en curso
    private BigDecimal ingresoConDescuentoMesActual; // suma de facturas del mes con descuento aplicado (referencia, no el monto exacto ahorrado)

    private List<TendenciaMensualDTO> tendenciaUltimos6Meses;

    @Data
    public static class MorosidadPorPlanDTO {
        private String nombrePlan; // "Sin plan asociado" para facturas manuales sin inscripción
        private long totalFacturas;
        private long facturasConSaldoPendiente;
        private Double porcentajeMorosidad; // null si el plan no tiene facturas todavía
    }

    @Data
    public static class TendenciaMensualDTO {
        private String mes; // "2026-06"
        private BigDecimal recaudado;
    }
}