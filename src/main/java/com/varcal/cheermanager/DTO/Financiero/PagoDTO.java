package com.varcal.cheermanager.DTO.Financiero;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class PagoDTO {

    private Integer facturaId;
    private Integer tipoPagoId;
    private Integer metodoPagoId;
    private LocalDate fecha;
    private String estado;       // "Pagado" | "Pendiente" | "Vencido" — ver enum EstadoPago
    private BigDecimal monto;

    // Getters y setters (Lombok @Data)
}
