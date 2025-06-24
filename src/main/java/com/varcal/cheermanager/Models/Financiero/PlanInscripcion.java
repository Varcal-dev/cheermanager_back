package com.varcal.cheermanager.Models.Financiero;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
@Table(name = "planes_inscripcion")
public class PlanInscripcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "precio_base_calculado", precision = 10, scale = 2)
    private BigDecimal precioBaseCalculado;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal precio;

    @Column(name = "vigencia_dias")
    private Integer vigenciaDias;

    @Column(name = "incluye_camiseta")
    private Boolean incluyeCamiseta;

    @Column(name = "incluye_seguro")
    private Boolean incluyeSeguro;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDate fechaCreacion;

    @Column(name = "fecha_vigencia_inicio", nullable = false)
    private LocalDate fechaVigenciaInicio;

    @Column(name = "fecha_vigencia_fin")
    private LocalDate fechaVigenciaFin;
}
