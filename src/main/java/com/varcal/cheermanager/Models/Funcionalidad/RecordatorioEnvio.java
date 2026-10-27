package com.varcal.cheermanager.Models.Funcionalidad;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Bitacora: un registro por factura, canal y destino. estado: ENVIADO | FALLIDO | SIN_DESTINO. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "recordatorios_envios")
public class RecordatorioEnvio {
    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "factura_id", nullable = false)
    private Integer facturaId;

    @Column(name = "persona_id", nullable = false)
    private Integer personaId;

    /** SISTEMA | CORREO | WHATSAPP */
    @Column(name = "canal", nullable = false, length = 12)
    private String canal;

    @Column(name = "destino", length = 160)
    private String destino;

    @Column(name = "estado", nullable = false, length = 12)
    private String estado;

    @Column(name = "detalle", length = 300)
    private String detalle;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "automatico", nullable = false)
    private Boolean automatico = false;

    @Column(name = "enviado_por_usuario_id")
    private Integer enviadoPorUsuarioId;
}
