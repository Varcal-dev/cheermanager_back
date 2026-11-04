package com.varcal.cheermanager.Models.Legal;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Aceptacion de un documento legal (tipo + version) por un club. Entidad de club (tenant). */
@Data
@NoArgsConstructor
@Entity
@Table(name = "aceptaciones_legales")
public class AceptacionLegal {

    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    /** TERMINOS | POLITICA */
    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "ip", length = 64)
    private String ip;
}
