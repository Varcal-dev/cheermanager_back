package com.varcal.cheermanager.Models.Evaluaciones;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "evaluacion_plantilla_criterios")
public class EvaluacionPlantillaCriterio {
    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "plantilla_id", nullable = false)
    private Integer plantillaId;

    @ManyToOne
    @JoinColumn(name = "criterio_id", nullable = false)
    private CriterioEvaluacion criterio;

    @Column(name = "orden", nullable = false)
    private Integer orden;
}
