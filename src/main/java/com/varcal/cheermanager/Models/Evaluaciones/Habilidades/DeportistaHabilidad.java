package com.varcal.cheermanager.Models.Evaluaciones.Habilidades;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.varcal.cheermanager.Models.Personas.Deportista;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Estado actual de un deportista en una habilidad (se actualiza con cada evaluacion). */
@Data
@NoArgsConstructor
@Entity
@Table(name = "deportista_habilidades")
public class DeportistaHabilidad {
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

    /** INICIAL | EN_DESARROLLO | DOMINADA (segun el puntaje de la ultima evaluacion). */
    @Column(name = "estado", nullable = false, length = 15)
    private String estado;

    @Column(name = "ultimo_puntaje", nullable = false)
    private BigDecimal ultimoPuntaje;

    @Column(name = "mejor_puntaje", nullable = false)
    private BigDecimal mejorPuntaje;

    @Column(name = "fecha_ultima", nullable = false)
    private LocalDate fechaUltima;

    @Column(name = "total_evaluaciones", nullable = false)
    private Integer totalEvaluaciones;
}
