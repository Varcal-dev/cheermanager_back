package com.varcal.cheermanager.Models.Evaluaciones.Habilidades;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Deduccion de un driver en una evaluacion: 0 = sin problema, 1 = -0.1, 2 = -0.2, 3 = -0.3. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "evaluacion_habilidad_driver")
public class EvaluacionHabilidadDriver {
    /** Club (tenant): Hibernate lo asigna solo al guardar y filtra por el toda consulta HQL/JPA. */
    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "evaluacion_habilidad_id", nullable = false)
    private EvaluacionHabilidad evaluacionHabilidad;

    @Column(name = "nombre_driver", nullable = false, length = 50)
    private String nombreDriver;

    @Column(name = "nivel_deduccion", nullable = false)
    private Integer nivelDeduccion;
}
