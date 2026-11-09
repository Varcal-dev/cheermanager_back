package com.varcal.cheermanager.Models.Legal;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Pre-registro de la landing. Entidad del club Plataforma (el proveedor), con la prueba del consentimiento. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "preregistros")
public class PreRegistro {

    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 120) private String nombre;
    @Column(nullable = false, length = 150) private String correo;
    @Column(length = 30) private String telefono;
    @Column(length = 150) private String club;
    @Column(length = 100) private String ciudad;
    private Integer deportistas;
    @Column(length = 80) private String rol;
    @Column(length = 1000) private String mensaje;

    /** Cuerpo recibido (recortado): conserva campos que aun no tienen columna propia. */
    @Column(name = "datos_json", length = 4000)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String datosJson;

    @Column(name = "version_consentimiento", nullable = false, length = 20) private String versionConsentimiento;
    @Column(name = "fecha_consentimiento", nullable = false) private LocalDateTime fechaConsentimiento;
    @Column(length = 64) private String ip;

    /** NUEVO | CONTACTADO | DESCARTADO */
    @Column(nullable = false, length = 20) private String estado = "NUEVO";
    @Column(length = 500) private String notas;
}
