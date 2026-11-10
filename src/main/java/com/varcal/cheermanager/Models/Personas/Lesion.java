package com.varcal.cheermanager.Models.Personas;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lesion de un deportista y su estado de retorno al juego (C3). Dato de salud. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "lesiones")
public class Lesion {

    public enum Zona { CABEZA_CUELLO, HOMBRO, CODO_MUNECA_MANO, ESPALDA, CADERA, RODILLA, TOBILLO_PIE, MUSCULAR, OTRA }
    public enum Gravedad { LEVE, MODERADA, GRAVE }
    /** Lo que puede hacer mientras tanto. */
    public enum Restriccion { SIN_ENTRENAR, MODIFICADO, SIN_RESTRICCION }
    public enum Estado { ACTIVA, EN_RETORNO, ALTA }

    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "deportista_id", nullable = false)
    private Integer deportistaId;

    @Column(name = "fecha_lesion", nullable = false)
    private LocalDate fechaLesion;

    @Enumerated(EnumType.STRING)
    @Column(name = "zona", nullable = false, length = 30)
    private Zona zona;

    @Column(name = "descripcion", nullable = false, length = 300)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "gravedad", nullable = false, length = 10)
    private Gravedad gravedad;

    @Enumerated(EnumType.STRING)
    @Column(name = "restriccion", nullable = false, length = 20)
    private Restriccion restriccion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 12)
    private Estado estado = Estado.ACTIVA;

    @Column(name = "fecha_estimada_retorno")
    private LocalDate fechaEstimadaRetorno;

    @Column(name = "fecha_alta")
    private LocalDate fechaAlta;

    @Column(name = "aval_medico", nullable = false)
    private boolean avalMedico;

    @Column(name = "aval_detalle", length = 150)
    private String avalDetalle;

    @Column(name = "registrado_por", nullable = false)
    private Integer registradoPor;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
