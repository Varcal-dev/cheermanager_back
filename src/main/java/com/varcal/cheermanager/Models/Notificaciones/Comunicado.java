package com.varcal.cheermanager.Models.Notificaciones;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mensaje del club a las familias de un grupo (o de todo el club). C7. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "comunicados")
public class Comunicado {

    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Vacio = todo el club. */
    @Column(name = "grupo_id")
    private Integer grupoId;

    @Column(name = "asunto", nullable = false, length = 150)
    private String asunto;

    @Column(name = "mensaje", nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "canal_sistema", nullable = false)
    private Boolean canalSistema;

    @Column(name = "canal_correo", nullable = false)
    private Boolean canalCorreo;

    @Column(name = "deportistas", nullable = false)
    private Integer deportistas = 0;

    @Column(name = "notificados", nullable = false)
    private Integer notificados = 0;

    @Column(name = "correos_enviados", nullable = false)
    private Integer correosEnviados = 0;

    @Column(name = "correos_fallidos", nullable = false)
    private Integer correosFallidos = 0;

    @Column(name = "sin_contacto", nullable = false)
    private Integer sinContacto = 0;

    @Column(name = "creado_por", nullable = false)
    private Integer creadoPor;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
}
