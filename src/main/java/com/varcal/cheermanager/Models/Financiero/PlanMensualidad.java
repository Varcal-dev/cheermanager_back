package com.varcal.cheermanager.Models.Financiero;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "planes_mensualidad")
public class PlanMensualidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "tipo_plan_id", nullable = false)
    private TipoPlanPago tipoPlan;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "valor_mensual", precision = 10, scale = 2, nullable = false)
    private BigDecimal valorMensual;

    @Column(name = "descuento_porcentaje", precision = 5, scale = 2)
    private BigDecimal descuentoPorcentaje;

    @Column(name = "sesiones_semanales")
    private Integer sesionesSemanales;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_vigencia_inicio", nullable = false)
    private LocalDate fechaVigenciaInicio;

    @Column(name = "fecha_vigencia_fin")
    private LocalDate fechaVigenciaFin;
}