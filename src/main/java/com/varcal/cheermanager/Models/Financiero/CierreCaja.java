package com.varcal.cheermanager.Models.Financiero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cierre de caja de un dia (uno por club y fecha). Guarda una FOTO de lo recaudado ese dia y el efectivo contado.
 * Con estado CERRADA el dia queda protegido: no se registran, confirman ni anulan pagos "Pagado" de esa fecha.
 * Reabrir (con motivo) deja el dia en REABIERTA: se puede corregir y se vuelve a cerrar (sube la revision).
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "cierres_caja", uniqueConstraints = {
        @UniqueConstraint(name = "uk_cierre_club_fecha", columnNames = { "club_id", "fecha" }) })
public class CierreCaja {
    public static final String CERRADA = "CERRADA";
    public static final String REABIERTA = "REABIERTA";

    /** Club (tenant): Hibernate lo asigna solo al guardar y filtra por el toda consulta HQL/JPA. */
    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "revision", nullable = false)
    private Integer revision = 1;

    @Column(name = "total_pagado", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalPagado;

    @Column(name = "efectivo_esperado", precision = 12, scale = 2, nullable = false)
    private BigDecimal efectivoEsperado;

    @Column(name = "efectivo_contado", precision = 12, scale = 2)
    private BigDecimal efectivoContado;

    @Column(name = "diferencia", precision = 12, scale = 2)
    private BigDecimal diferencia;

    /** Foto del recaudo por metodo y por tipo, en JSON (ver CajaService). */
    @Column(name = "detalle_json", length = 4000)
    private String detalleJson;

    @Column(name = "nota", length = 300)
    private String nota;

    @Column(name = "cerrado_por_usuario_id")
    private Integer cerradoPorUsuarioId;

    @Column(name = "cerrado_en")
    private LocalDateTime cerradoEn;

    @Column(name = "reabierto_por_usuario_id")
    private Integer reabiertoPorUsuarioId;

    @Column(name = "reabierto_en")
    private LocalDateTime reabiertoEn;

    @Column(name = "motivo_reapertura", length = 300)
    private String motivoReapertura;
}
