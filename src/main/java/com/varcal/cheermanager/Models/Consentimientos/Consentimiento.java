package com.varcal.cheermanager.Models.Consentimientos;

import java.time.LocalDateTime;

import com.varcal.cheermanager.Models.Personas.Acudiente;
import com.varcal.cheermanager.Models.Personas.Persona;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Autorizacion para el tratamiento de datos personales (Ley 1581 de 2012). Es un REGISTRO DE EVIDENCIA:
 * no se edita ni se borra. Revocar = poner fecha_revocado; un nuevo otorgamiento crea otra fila.
 * Titular = la persona a quien pertenecen los datos. Si es menor de edad, otorga su acudiente.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "consentimientos", indexes = {
        @Index(name = "idx_consent_titular_tipo", columnList = "club_id, titular_id, tipo")})
public class Consentimiento {
    /** Club (tenant): Hibernate lo asigna solo al guardar y filtra por el toda consulta HQL/JPA. */
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    public enum Tipo {
        /** Tratamiento general de datos personales. */
        TRATAMIENTO_DATOS,
        /** Datos sensibles de salud (historial medico, lesiones, alergias). */
        DATOS_SALUD,
        /** Uso de fotos y videos del menor (redes, publicidad). */
        IMAGEN
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "titular_id", nullable = false)
    private Persona titular;

    /** Quien autoriza cuando el titular es menor. Null si el titular es mayor y autoriza el mismo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acudiente_id")
    private Acudiente acudiente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30, columnDefinition = "varchar(30)")
    private Tipo tipo;

    /** Version del texto de autorizacion aceptado (ej. "2026-01"): permite saber QUE se firmo. */
    @Column(name = "version_texto", nullable = false, length = 40)
    private String versionTexto;

    /** PAPEL, DIGITAL, VERBAL_GRABADO... Como se obtuvo; el soporte (firma) se conserva aparte. */
    @Column(name = "canal", nullable = false, length = 30)
    private String canal;

    @Column(name = "fecha_otorgado", nullable = false)
    private LocalDateTime fechaOtorgado;

    @Column(name = "fecha_revocado")
    private LocalDateTime fechaRevocado;

    @Column(name = "motivo_revocacion", length = 300)
    private String motivoRevocacion;

    /** Usuario del club que lo registro (trazabilidad). */
    @Column(name = "registrado_por_usuario_id")
    private Integer registradoPorUsuarioId;

    @Column(name = "observaciones", length = 300)
    private String observaciones;
}