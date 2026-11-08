package com.varcal.cheermanager.Models.Personas;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Documento con vencimiento de un deportista (C4). Solo fechas y referencia: no el contenido medico. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "documentos_deportista")
public class DocumentoDeportista {

    public enum Tipo { CERTIFICADO_MEDICO, EPS, SEGURO, LICENCIA }

    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "deportista_id", nullable = false)
    private Integer deportistaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private Tipo tipo;

    @Column(name = "entidad", length = 120)
    private String entidad;

    @Column(name = "numero", length = 60)
    private String numero;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    /** Vacio = no vence (por ejemplo una afiliacion a EPS vigente). */
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "observaciones", length = 300)
    private String observaciones;

    @Column(name = "registrado_por", nullable = false)
    private Integer registradoPor;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
