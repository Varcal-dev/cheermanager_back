package com.varcal.cheermanager.DTO.Financiero;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class FacturaDTO {
    
    private String numeroFactura;
    private LocalDate fechaEmision;
    private Integer personaId;
    private String descripcion;
    private Integer descuentoId;
    private BigDecimal total;

    // Getters y setters
}
