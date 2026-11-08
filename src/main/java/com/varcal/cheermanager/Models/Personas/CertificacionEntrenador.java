package com.varcal.cheermanager.Models.Personas;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Certificacion de un entrenador (primeros auxilios, seguridad deportiva, cursos de federacion...). C10. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "certificaciones_entrenador")
public class CertificacionEntrenador {

    public enum Tipo { PRIMEROS_AUXILIOS, RCP, SEGURIDAD_DEPORTIVA, CURSO_FEDERACION, OTRA }

    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "entrenador_id", nullable = false)
    private Integer entrenadorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 24)
    private Tipo tipo;

    /** Nombre del curso (obligatorio para CURSO_FEDERACION y OTRA). */
    @Column(name = "nombre", length = 120)
    private String nombre;

    @Column(name = "entidad", length = 120)
    private String entidad;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "registrado_por", nullable = false)
    private Integer registradoPor;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
