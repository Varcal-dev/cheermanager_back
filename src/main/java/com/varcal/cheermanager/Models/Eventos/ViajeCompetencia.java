package com.varcal.cheermanager.Models.Eventos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Viaje del club a un evento (C5). Uno por evento. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "viajes_competencia")
public class ViajeCompetencia {

    public enum Estado { ABIERTO, CERRADO }

    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "evento_id", nullable = false) private Integer eventoId;
    @Column(name = "destino", nullable = false, length = 150) private String destino;
    @Column(name = "fecha_salida", nullable = false) private LocalDate fechaSalida;
    @Column(name = "fecha_regreso", nullable = false) private LocalDate fechaRegreso;
    @Column(name = "punto_encuentro", length = 150) private String puntoEncuentro;
    @Column(name = "transporte", length = 150) private String transporte;
    @Column(name = "alojamiento", length = 200) private String alojamiento;
    @Column(name = "costo_por_persona", precision = 12, scale = 2) private BigDecimal costoPorPersona;
    @Column(name = "notas", length = 500) private String notas;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private Estado estado = Estado.ABIERTO;

    @Column(name = "creado_por", nullable = false) private Integer creadoPor;
    @Column(name = "fecha_creacion", nullable = false) private LocalDateTime fechaCreacion;
}
