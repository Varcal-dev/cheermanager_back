package com.varcal.cheermanager.Models.Eventos;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Deportista dentro de un roster, como titular o suplente. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "roster_integrantes")
public class RosterIntegrante {

    public enum Rol { TITULAR, SUPLENTE }

    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "roster_id", nullable = false)
    private Integer rosterId;

    @Column(name = "deportista_id", nullable = false)
    private Integer deportistaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 10)
    private Rol rol;

    /** Posicion en la rutina (base, flyer, backspot...). Texto libre. */
    @Column(name = "posicion", length = 40)
    private String posicion;

    @Column(name = "fecha_agregado", nullable = false)
    private LocalDateTime fechaAgregado;
}
