package com.varcal.cheermanager.Models.Evaluaciones;

import java.time.LocalDate;

import com.varcal.cheermanager.Models.Personas.Deportista;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "evaluaciones")
public class Evaluacion {
    /** Club (tenant): Hibernate lo asigna solo al guardar y filtra por el toda consulta HQL/JPA. */
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "deportista_id", nullable = false)
    private Deportista deportista;

    @ManyToOne
    @JoinColumn(name = "criterio_id", nullable = false)
    private CriterioEvaluacion criterioEvaluacion;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "puntaje_obtenido", nullable = false)
    private Integer puntajeObtenido;
}