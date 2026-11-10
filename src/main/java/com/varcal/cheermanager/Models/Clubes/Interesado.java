package com.varcal.cheermanager.Models.Clubes;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Persona interesada en entrar al club (C6). Llega por el formulario publico; datos del contacto y del posible deportista. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "interesados")
public class Interesado {

    public enum Tipo { CLASE_PRUEBA, INSCRIPCION, LISTA_ESPERA }
    public enum Estado { NUEVO, CONTACTADO, PRUEBA_AGENDADA, ASISTIO, EN_ESPERA, INSCRITO, DESCARTADO }

    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 15)
    private Tipo tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 16)
    private Estado estado;

    @Column(name = "contacto_nombre", nullable = false, length = 120) private String contactoNombre;
    @Column(name = "contacto_correo", nullable = false, length = 150) private String contactoCorreo;
    @Column(name = "contacto_telefono", nullable = false, length = 30) private String contactoTelefono;
    @Column(name = "deportista_nombre", nullable = false, length = 120) private String deportistaNombre;
    @Column(name = "deportista_nacimiento", nullable = false) private LocalDate deportistaNacimiento;
    @Column(name = "experiencia", length = 300) private String experiencia;
    @Column(name = "mensaje", length = 500) private String mensaje;

    /** Grupo al que se piensa ubicar o en cuya espera esta (lo asigna el club). */
    @Column(name = "grupo_id") private Integer grupoId;
    @Column(name = "fecha_prueba") private LocalDate fechaPrueba;
    @Column(name = "notas", length = 500) private String notas;

    @Column(name = "version_consentimiento", nullable = false, length = 20) private String versionConsentimiento;
    @Column(name = "fecha_consentimiento", nullable = false) private LocalDateTime fechaConsentimiento;
    @Column(name = "ip", length = 64) private String ip;
    @Column(name = "fecha_creacion", nullable = false) private LocalDateTime fechaCreacion;
    @Column(name = "fecha_actualizacion", nullable = false) private LocalDateTime fechaActualizacion;
}
