package com.varcal.cheermanager.Models.Financiero;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "planes_pago")
public class PlanPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "tipo_plan_id", nullable = false)
    private TipoPlanPago tipoPlan;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "valor_mensual", precision = 10, scale = 2, nullable = false)
    private BigDecimal valorMensual;

    @Column(name = "fecha_vigencia_inicio", nullable = false)
    private LocalDate fechaVigenciaInicio;

    @Column(name = "fecha_vigencia_fin")
    private LocalDate fechaVigenciaFin;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}