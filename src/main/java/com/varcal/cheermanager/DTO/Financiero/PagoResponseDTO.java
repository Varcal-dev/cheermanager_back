package com.varcal.cheermanager.DTO.Financiero;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class PagoResponseDTO {

    private Integer id;

    private Integer facturaId;
    private String numeroFactura;
    private BigDecimal totalFactura;

    private Integer tipoPagoId;
    private String tipoPago;

    private Integer metodoPagoId;
    private String metodoPago;

    private LocalDate fecha;
    private String estado;
    private BigDecimal monto;

    // Saldo restante de la factura DESPUÉS de aplicar este pago (y los demás
    // pagos "Pagado" que ya tenía). Le ahorra al frontend tener que calcularlo.
    private BigDecimal saldoPendienteFactura;
}