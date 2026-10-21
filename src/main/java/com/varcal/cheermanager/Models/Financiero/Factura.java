package com.varcal.cheermanager.Models.Financiero;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.varcal.cheermanager.Models.Org_dep.Inscripcion;
import com.varcal.cheermanager.Models.Personas.Persona;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "facturas", uniqueConstraints = {@jakarta.persistence.UniqueConstraint(name = "uk_factura_club_numero_factura", columnNames = {"club_id", "numero_factura"})})
public class Factura {
    /** Club (tenant): Hibernate lo asigna solo al guardar y filtra por el toda consulta HQL/JPA. */
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "numero_factura", nullable = false)
    private String numeroFactura;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @ManyToOne
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @Column(name = "descripcion")
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "descuento_id", nullable = true)
    private Descuento descuento;

    @Column(name = "total", precision = 10, scale = 2, nullable = false)
    private BigDecimal total;

    // Opcional: se llena cuando la factura nace automáticamente de una
    // inscripción (ver InscripcionService.crearInscripcion). Las facturas
    // creadas manualmente (POST /api/facturas) seguirán teniendo esto en null,
    // así que no rompe nada existente.
    @ManyToOne
    @JoinColumn(name = "inscripcion_id", nullable = true)
    private Inscripcion inscripcion;

    // Nuevo. Formato "yyyy-MM". Solo se llena en facturas de MENSUALIDAD:
    // identifica a qué mes corresponde el cobro, para no volver a facturar
    // el mismo mes dos veces (ver InscripcionService.generarCobrosMensualidad).
    // Las facturas de inscripción (cobro único) y las manuales quedan null.
    @Column(name = "periodo", length = 7)
    private String periodo;

    /** Fecha limite de pago: emision + 10 dias. Pasada esa fecha, con saldo pendiente, esta EN MORA. */
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    /** INSCRIPCION, MENSUALIDAD, TIENDA, UNIFORME, COMPETENCIA, EVENTO u OTRO. */
    @Column(name = "concepto", length = 30)
    private String concepto;

    /** Evento al que se liga un cobro manual (opcional). */
    @Column(name = "evento_id")
    private Integer eventoId;

    /** Las facturas no se borran: se anulan (no cuentan en cartera ni en recaudo). */
    @Column(name = "anulada", nullable = false)
    private Boolean anulada = false;

    @Column(name = "fecha_anulacion")
    private java.time.LocalDateTime fechaAnulacion;

    @Column(name = "motivo_anulacion", length = 300)
    private String motivoAnulacion;

    @Column(name = "anulada_por_usuario_id")
    private Integer anuladaPorUsuarioId;

}