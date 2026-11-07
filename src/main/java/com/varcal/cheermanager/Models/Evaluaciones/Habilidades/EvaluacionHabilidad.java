package com.varcal.cheermanager.Models.Evaluaciones.Habilidades;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.varcal.cheermanager.Models.Personas.Deportista;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Una evaluacion de una habilidad: valor inicial menos las deducciones de los drivers aplicados. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "evaluaciones_habilidad")
public class EvaluacionHabilidad {
    /** Club (tenant): Hibernate lo asigna solo al guardar y filtra por el toda consulta HQL/JPA. */
    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "deportista_id", nullable = false)
    private Deportista deportista;

    @ManyToOne(optional = false)
    @JoinColumn(name = "habilidad_id", nullable = false)
    private HabilidadCatalogo habilidad;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "usuario_id")
    private Integer usuarioId;

    @Column(name = "valor_inicial", nullable = false)
    private BigDecimal valorInicial;

    @Column(name = "total_deducciones", nullable = false)
    private BigDecimal totalDeducciones;

    @Column(name = "puntaje", nullable = false)
    private BigDecimal puntaje;

    @Column(name = "observaciones", length = 500)
    private String observaciones;
}
