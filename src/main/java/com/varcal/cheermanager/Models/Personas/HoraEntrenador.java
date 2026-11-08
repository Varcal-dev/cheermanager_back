package com.varcal.cheermanager.Models.Personas;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Horas dictadas por un entrenador en una fecha (manuales o precargadas desde los horarios). C10. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "horas_entrenador")
public class HoraEntrenador {

    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "entrenador_id", nullable = false)
    private Integer entrenadorId;

    @Column(name = "grupo_id")
    private Integer grupoId;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "horas", nullable = false, precision = 4, scale = 2)
    private BigDecimal horas;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    /** MANUAL o HORARIO (precargada). */
    @Column(name = "origen", nullable = false, length = 10)
    private String origen;

    @Column(name = "registrado_por", nullable = false)
    private Integer registradoPor;
}
