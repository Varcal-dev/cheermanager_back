package com.varcal.cheermanager.Models.Financiero;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "pagos")
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;

    @Column(name = "tipo_pago_id", nullable = false)
    private Integer tipoPagoId;

    @Column(name = "metodo_pago_id", nullable = false)
    private Integer metodoPagoId;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "estado_id", nullable = false)
    private Integer estadoId;

    @Column(name = "monto", precision = 10, scale = 2, nullable = false)
    private BigDecimal monto;
}