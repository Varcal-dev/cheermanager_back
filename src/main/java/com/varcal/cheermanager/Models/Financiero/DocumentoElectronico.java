package com.varcal.cheermanager.Models.Financiero;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Una factura (o nota credito) electronica enviada al proveedor (V45). */
@Data
@NoArgsConstructor
@Entity
@Table(name = "documentos_electronicos")
public class DocumentoElectronico {
    public static final String PENDIENTE = "PENDIENTE";
    public static final String EMITIDA = "EMITIDA";
    public static final String RECHAZADA = "RECHAZADA";
    public static final String ERROR = "ERROR";

    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "factura_id", nullable = false)
    private Integer facturaId;

    @Column(name = "pago_id")
    private Integer pagoId;

    /** FACTURA o NOTA_CREDITO. */
    @Column(name = "tipo", nullable = false, length = 15)
    private String tipo = "FACTURA";

    @Column(name = "estado", nullable = false, length = 12)
    private String estado = PENDIENTE;

    /** Referencia idempotente enviada al proveedor: reintentar con la misma no duplica el documento. */
    @Column(name = "referencia", nullable = false, length = 60)
    private String referencia;

    @Column(name = "ambiente", nullable = false, length = 12)
    private String ambiente = "PRUEBAS";

    @Column(name = "numero", length = 30)
    private String numero;

    @Column(name = "cufe", length = 160)
    private String cufe;

    @Column(name = "url_pdf", length = 400)
    private String urlPdf;

    @Column(name = "mensaje_error", length = 500)
    private String mensajeError;

    @Column(name = "intentos", nullable = false)
    private Integer intentos = 0;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "emitido_en")
    private LocalDateTime emitidoEn;
}
