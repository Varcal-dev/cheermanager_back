package com.varcal.cheermanager.Models.Personas;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Asistencia del personal: entrada y salida de un entrenador en un dia. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "asistencia_entrenador")
public class AsistenciaEntrenador {
    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "entrenador_id", nullable = false)
    private Integer entrenadorId;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_entrada", nullable = false)
    private LocalTime horaEntrada;

    @Column(name = "hora_salida")
    private LocalTime horaSalida;

    /** ESCANEO o MANUAL. */
    @Column(name = "origen", nullable = false, length = 10)
    private String origen = "ESCANEO";

    @Column(name = "registrado_por")
    private Integer registradoPor;
}
