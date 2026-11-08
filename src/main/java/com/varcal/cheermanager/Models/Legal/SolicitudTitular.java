package com.varcal.cheermanager.Models.Legal;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Consulta o reclamo de un titular de datos personales (Ley 1581, arts. 14 y 15). L8. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "solicitudes_titular")
public class SolicitudTitular {

    /** CONSULTA: 10 dias habiles (prorroga 5). Las demas son reclamos: 15 dias habiles (prorroga 8). */
    public enum Tipo { CONSULTA, ACTUALIZACION, SUPRESION, REVOCATORIA, RECLAMO }

    public enum Estado { RECIBIDA, EN_TRAMITE, RESPONDIDA }

    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private Tipo tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private Estado estado = Estado.RECIBIDA;

    @Column(name = "canal", nullable = false, length = 12)
    private String canal;

    @Column(name = "solicitante_nombre", nullable = false, length = 160)
    private String solicitanteNombre;

    @Column(name = "solicitante_documento", length = 40)
    private String solicitanteDocumento;

    @Column(name = "solicitante_contacto", length = 160)
    private String solicitanteContacto;

    @Column(name = "persona_id")
    private Integer personaId;

    @Column(name = "descripcion", nullable = false, length = 2000)
    private String descripcion;

    @Column(name = "fecha_recepcion", nullable = false)
    private LocalDate fechaRecepcion;

    @Column(name = "fecha_limite", nullable = false)
    private LocalDate fechaLimite;

    @Column(name = "prorrogada", nullable = false)
    private Boolean prorrogada = false;

    @Column(name = "motivo_prorroga", length = 500)
    private String motivoProrroga;

    @Column(name = "respuesta", length = 2000)
    private String respuesta;

    @Column(name = "fecha_respuesta")
    private LocalDate fechaRespuesta;

    @Column(name = "registrada_por", nullable = false)
    private Integer registradaPor;

    @Column(name = "respondida_por")
    private Integer respondidaPor;
}
