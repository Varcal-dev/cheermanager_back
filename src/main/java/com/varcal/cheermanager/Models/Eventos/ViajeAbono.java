package com.varcal.cheermanager.Models.Eventos;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Abono de un participante al costo del viaje. Es un registro interno (no genera factura). */
@Data
@NoArgsConstructor
@Entity
@Table(name = "viaje_abonos")
public class ViajeAbono {
    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "participante_id", nullable = false) private Integer participanteId;
    @Column(name = "monto", nullable = false, precision = 12, scale = 2) private BigDecimal monto;
    @Column(name = "fecha", nullable = false) private LocalDate fecha;
    @Column(name = "nota", length = 200) private String nota;
    @Column(name = "registrado_por", nullable = false) private Integer registradoPor;
}
