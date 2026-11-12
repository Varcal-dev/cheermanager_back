package com.varcal.cheermanager.Models.Evaluaciones;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Conjunto de criterios con nombre (p. ej. "Tryout Elite 2027") que se reutiliza para evaluar a varios deportistas. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "evaluacion_plantillas")
public class EvaluacionPlantilla {
    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo = "TRYOUT";

    /** Prep | Elite | null (sin criterios de rúbrica). Novice no tiene rúbrica de evaluación. */
    @Column(name = "modalidad", length = 20)
    private String modalidad;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();
}
