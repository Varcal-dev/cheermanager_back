package com.varcal.cheermanager.Models.Personas;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Nota de seguimiento de una lesion; guarda el estado y la restriccion que quedaron tras la nota. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "lesion_seguimientos")
public class LesionSeguimiento {

    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "lesion_id", nullable = false)
    private Integer lesionId;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "nota", nullable = false, length = 500)
    private String nota;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 12)
    private Lesion.Estado estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "restriccion", nullable = false, length = 20)
    private Lesion.Restriccion restriccion;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
