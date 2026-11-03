package com.varcal.cheermanager.Models.Financiero;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "pagos")
public class Pago {
    /** Club (tenant): Hibernate lo asigna solo al guardar y filtra por el toda consulta HQL/JPA. */
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;

    @ManyToOne
    @JoinColumn(name = "tipo_pago_id", nullable = false)
    private TipoPago tipoPago;

    @ManyToOne
    @JoinColumn(name = "metodo_pago_id", nullable = false)
    private MetodoPago metodoPago;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "estado", nullable = false, columnDefinition = "varchar(50)")
    @Enumerated(EnumType.STRING)
    private EstadoPago estado;

    @Column(name = "monto", precision = 10, scale = 2, nullable = false)
    private BigDecimal monto;

    // Anulacion (estado = Anulado): el pago no se borra, queda con motivo, fecha y usuario.
    @Column(name = "fecha_anulacion")
    private java.time.LocalDateTime fechaAnulacion;

    @Column(name = "motivo_anulacion", length = 300)
    private String motivoAnulacion;

    @Column(name = "anulado_por_usuario_id")
    private Integer anuladoPorUsuarioId;

    // Caja y conciliacion (V15). Los pagos anteriores a V15 tienen estos campos en null.
    /** Usuario que registro el pago. */
    @Column(name = "registrado_por_usuario_id")
    private Integer registradoPorUsuarioId;

    /** Momento real del registro (la fecha del pago la elige quien registra; esta no). */
    @Column(name = "creado_en")
    private java.time.LocalDateTime creadoEn;

    /** Numero de comprobante o referencia del banco (transferencia, Nequi, PSE, tarjeta). */
    @Column(name = "referencia", length = 100)
    private String referencia;

    /** Se llena cuando el pago se cruzo con el extracto del banco. */
    @Column(name = "conciliado_en")
    private java.time.LocalDateTime conciliadoEn;

    @Column(name = "conciliado_por_usuario_id")
    private Integer conciliadoPorUsuarioId;

}
