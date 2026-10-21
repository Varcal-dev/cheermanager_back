package com.varcal.cheermanager.Models.Org_dep;

import java.time.LocalDateTime;

import com.varcal.cheermanager.Models.Personas.Deportista;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Solicitud de un deportista/acudiente para inscribirse a una temporada. El club la aprueba o rechaza una por una;
 * al aprobar se crea la inscripcion real (con sus facturas).
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "solicitudes_inscripcion")
public class SolicitudInscripcion {
    public enum Estado { PENDIENTE, APROBADA, RECHAZADA, CANCELADA }

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
    @JoinColumn(name = "temporada_id", nullable = false)
    private Temporada temporada;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, columnDefinition = "varchar(20)")
    private Estado estado = Estado.PENDIENTE;

    @Column(name = "observaciones", length = 300)
    private String observaciones;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "solicitada_por_usuario_id")
    private Integer solicitadaPorUsuarioId;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(name = "resuelta_por_usuario_id")
    private Integer resueltaPorUsuarioId;

    @Column(name = "motivo_resolucion", length = 300)
    private String motivoResolucion;

    @Column(name = "inscripcion_id")
    private Integer inscripcionId;
}
