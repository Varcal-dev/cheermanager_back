package com.varcal.cheermanager.Models.Auth;

import java.time.LocalDateTime;
import java.util.Set;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.varcal.cheermanager.Models.Personas.Persona;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
// Corregido: la tabla es "usuarios" (antes decia "usuario" y el borrado fallaba).
@SQLDelete(sql = "UPDATE usuarios SET activo = false WHERE id = ?")
// @Where esta deprecado desde Hibernate 6.3.
@SQLRestriction("activo = true")
@Table(name = "usuarios")
public class Usuario {
    /** Club (tenant): Hibernate lo asigna solo al guardar y filtra por el toda consulta HQL/JPA. */
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    // @JsonIgnore: ningun endpoint que serialice la entidad puede filtrar el hash.
    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "bloqueado", nullable = false)
    private Boolean bloqueado = false;

    /** Hasta cuando dura el bloqueo temporal. null = no hay bloqueo temporal. */
    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos = 0;

    /** Se guarda el HASH (SHA-256) del token de recuperacion, nunca el token en claro. */
    @JsonIgnore
    @Column(name = "token_reset_password")
    private String tokenResetPassword;

    @JsonIgnore
    @Column(name = "token_reset_password_expiracion")
    private LocalDateTime tokenResetPasswordExpiracion;

    // Se activa cuando el usuario fue creado con una contrasena por defecto
    // o tras un reset administrativo. Se limpia en el primer cambio exitoso.
    @Column(name = "requiere_cambio_password", nullable = false)
    private Boolean requiereCambioPassword = false;

    @ManyToOne
    @JoinColumn(name = "rol_id")
    private Rol rol;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "usuario_roles",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private Set<Rol> roles;

    @ManyToOne
    @JoinColumn(name = "persona_id")
    private Persona persona;
}
