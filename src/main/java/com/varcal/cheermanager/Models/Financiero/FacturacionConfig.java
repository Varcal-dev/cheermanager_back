package com.varcal.cheermanager.Models.Financiero;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Datos fiscales del club como emisor de factura electronica (V45). Una fila por club; nace apagada. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "facturacion_config")
public class FacturacionConfig {
    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "activa", nullable = false)
    private Boolean activa = false;

    /** FACTUS hoy; el motor propio podria ser otro valor mas adelante. */
    @Column(name = "proveedor", nullable = false, length = 20)
    private String proveedor = "FACTUS";

    /** PRUEBAS (habilitacion / sandbox) o PRODUCCION. */
    @Column(name = "ambiente", nullable = false, length = 12)
    private String ambiente = "PRUEBAS";

    /** Cuando se emite: PAGO (al registrar el pago) o COBRO (al crear el cobro). */
    @Column(name = "emitir_al", nullable = false, length = 10)
    private String emitirAl = "PAGO";

    /** NATURAL o JURIDICA. */
    @Column(name = "tipo_persona", length = 10)
    private String tipoPersona;

    @Column(name = "nit", length = 15)
    private String nit;

    @Column(name = "dv", length = 1)
    private String dv;

    @Column(name = "razon_social", length = 160)
    private String razonSocial;

    @Column(name = "nombre_comercial", length = 160)
    private String nombreComercial;

    @Column(name = "responsable_iva", nullable = false)
    private Boolean responsableIva = false;

    /** EXCLUIDO (los servicios no causan IVA) o GRAVADO (con tarifaIva). Lo define el contador. */
    @Column(name = "tratamiento_iva", nullable = false, length = 10)
    private String tratamientoIva = "EXCLUIDO";

    @Column(name = "tarifa_iva", nullable = false, precision = 5, scale = 2)
    private BigDecimal tarifaIva = BigDecimal.ZERO;

    @Column(name = "municipio_codigo", length = 10)
    private String municipioCodigo;

    @Column(name = "direccion", length = 200)
    private String direccion;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "email", length = 150)
    private String email;

    /** Id del rango de numeracion (resolucion DIAN) configurado en el proveedor. */
    @Column(name = "rango_numeracion_id")
    private Integer rangoNumeracionId;

    @Column(name = "prefijo", length = 10)
    private String prefijo;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;
}
