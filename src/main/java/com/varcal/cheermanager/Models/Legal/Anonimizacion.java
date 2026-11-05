package com.varcal.cheermanager.Models.Legal;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Constancia de una anonimizacion (L7). No guarda datos personales de la persona anonimizada. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "anonimizaciones")
public class Anonimizacion {

    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "persona_id", nullable = false)
    private Integer personaId;

    @Column(name = "usuario_id", nullable = false)
    private Integer usuarioId;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "motivo", nullable = false, length = 300)
    private String motivo;
}
