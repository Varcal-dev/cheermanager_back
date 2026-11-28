package com.varcal.cheermanager.DTO.Financiero;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class PagoDTO {

    @NotNull(message = "Indica la factura")
    private Integer facturaId;
    @NotNull(message = "Indica el tipo de pago")
    private Integer tipoPagoId;
    @NotNull(message = "Indica el metodo de pago")
    private Integer metodoPagoId;
    @NotNull(message = "La fecha del pago es obligatoria")
    private LocalDate fecha;
    @NotBlank(message = "Indica el estado del pago")
    private String estado;       // "Pagado" | "Pendiente" | "Vencido" — ver enum EstadoPago
    @NotNull(message = "El monto del pago es obligatorio")
    @Positive(message = "El monto del pago debe ser mayor a cero")
    private BigDecimal monto;
    /** Opcional: comprobante o referencia del banco. */
    private String referencia;

    // Getters y setters (Lombok @Data)
}
