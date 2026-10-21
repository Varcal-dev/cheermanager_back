package com.varcal.cheermanager.DTO.Financiero;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

// Reemplaza el uso de la entidad Factura cruda en las respuestas de
// FacturaController. Antes, GET /api/facturas devolvía el objeto Factura
// completo, incluyendo factura.inscripcion.deportista.historialGrupos...
// (todo el grafo de JPA). Eso es pesado y, si algún día se reintroduce
// alguna relación bidireccional sin @JsonIgnore, vuelve a romperse. Con este
// DTO, la respuesta queda plana y controlada explícitamente.
@Data
public class FacturaResponseDTO {

    private Integer id;
    private String numeroFactura;
    private LocalDate fechaEmision;
    private PersonaResumenDTO persona;
    private String descripcion;
    private DescuentoResumenDTO descuento; // null si la factura no tiene descuento de convenio
    private BigDecimal total;
    private Integer inscripcionId; // solo el id, nunca el objeto Inscripcion completo

    // --- Cartera (calculada en el servidor; el frontend ya no la deduce) ---
    private LocalDate fechaVencimiento;
    private String concepto;
    private Integer eventoId;
    private Boolean anulada;
    private String motivoAnulacion;
    private BigDecimal totalPagado;     // suma de pagos en estado "Pagado"
    private BigDecimal saldoPendiente;  // total - totalPagado (0 si esta anulada)
    /** PAGADA | ABONO_PARCIAL | PENDIENTE | EN_MORA | ANULADA */
    private String estadoCuenta;
    private Long diasMora;              // solo si EN_MORA


    @Data
    public static class PersonaResumenDTO {
        private Integer id;
        private String nombre;
        private String apellidos;
    }

    @Data
    public static class DescuentoResumenDTO {
        private Integer id;
        private String nombre;
        private String descripcion;
        private BigDecimal porcentaje;
        private Boolean activo;
    }
}