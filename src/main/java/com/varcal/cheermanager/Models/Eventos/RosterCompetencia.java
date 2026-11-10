package com.varcal.cheermanager.Models.Eventos;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Roster de un grupo para un evento (C2). */
@Data
@NoArgsConstructor
@Entity
@Table(name = "rosters_competencia")
public class RosterCompetencia {

    public enum Estado { BORRADOR, CONFIRMADO }

    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "evento_id", nullable = false)
    private Integer eventoId;

    @Column(name = "grupo_id", nullable = false)
    private Integer grupoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 12)
    private Estado estado = Estado.BORRADOR;

    /** Maximo de titulares (null = sin tope). */
    @Column(name = "cupo_maximo")
    private Integer cupoMaximo;

    @Column(name = "creado_por", nullable = false)
    private Integer creadoPor;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;
}
