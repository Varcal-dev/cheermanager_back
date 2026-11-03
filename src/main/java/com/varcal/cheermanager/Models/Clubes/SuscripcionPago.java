package com.varcal.cheermanager.Models.Clubes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cobro (manual) de la suscripcion de un club. Tabla de PLATAFORMA: lleva club_id como dato, pero NO es una
 * entidad de club (sin @TenantId): solo la lee el superadmin con gestionar_clubes.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "suscripciones_pagos")
public class SuscripcionPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "club_id", nullable = false)
    private Integer clubId;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "meses", nullable = false)
    private Integer meses;

    @Column(name = "metodo", length = 40)
    private String metodo;

    @Column(name = "referencia", length = 120)
    private String referencia;

    @Column(name = "periodo_desde", nullable = false)
    private LocalDate periodoDesde;

    @Column(name = "periodo_hasta", nullable = false)
    private LocalDate periodoHasta;

    @Column(name = "nota", length = 300)
    private String nota;

    @Column(name = "registrado_por", length = 80)
    private String registradoPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
}
