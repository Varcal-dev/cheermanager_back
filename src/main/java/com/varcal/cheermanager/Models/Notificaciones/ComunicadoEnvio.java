package com.varcal.cheermanager.Models.Notificaciones;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Resultado de un envio puntual de un comunicado (un correo o una notificacion del portal). C7. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "comunicado_envios")
public class ComunicadoEnvio {

    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "comunicado_id", nullable = false)
    private Integer comunicadoId;

    /** SISTEMA o CORREO. */
    @Column(name = "canal", nullable = false, length = 10)
    private String canal;

    @Column(name = "destino", nullable = false, length = 160)
    private String destino;

    /** ENVIADO o FALLIDO. */
    @Column(name = "estado", nullable = false, length = 10)
    private String estado;

    @Column(name = "detalle", length = 200)
    private String detalle;
}
