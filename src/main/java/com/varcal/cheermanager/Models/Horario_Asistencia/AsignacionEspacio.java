package com.varcal.cheermanager.Models.Horario_Asistencia;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Espacio donde se realiza un horario de entrenamiento de un grupo. Tabla aparte: no toca los horarios existentes. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "asignaciones_espacio")
public class AsignacionEspacio {
    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "horario_entrenamiento_id", nullable = false) private Integer horarioEntrenamientoId;
    @Column(name = "espacio_id", nullable = false) private Integer espacioId;
}
