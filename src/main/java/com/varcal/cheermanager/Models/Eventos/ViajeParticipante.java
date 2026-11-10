package com.varcal.cheermanager.Models.Eventos;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Persona que viaja: un deportista de la delegacion o un acompanante (entrenador, acudiente, otro). */
@Data
@NoArgsConstructor
@Entity
@Table(name = "viaje_participantes")
public class ViajeParticipante {

    public enum Tipo { DEPORTISTA, ACOMPANANTE }
    public enum Estado { PENDIENTE, CONFIRMADO, NO_VA }
    public enum RolAcompanante { ENTRENADOR, ACUDIENTE, OTRO }

    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "viaje_id", nullable = false) private Integer viajeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 12)
    private Tipo tipo;

    @Column(name = "deportista_id") private Integer deportistaId;
    @Column(name = "acompanante_nombre", length = 120) private String acompananteNombre;
    @Column(name = "acompanante_documento", length = 30) private String acompananteDocumento;

    @Enumerated(EnumType.STRING)
    @Column(name = "acompanante_rol", length = 12)
    private RolAcompanante acompananteRol;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 12)
    private Estado estado = Estado.PENDIENTE;

    /** Carta de autorizacion de viaje firmada por el acudiente (obligatoria para menores de edad). */
    @Column(name = "autorizacion_viaje", nullable = false) private boolean autorizacionViaje;
    @Column(name = "fecha_autorizacion") private LocalDate fechaAutorizacion;
    @Column(name = "notas", length = 300) private String notas;
}
