package com.varcal.cheermanager.Models.Auth;

import java.time.LocalDateTime;
import java.util.Set;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@SQLDelete(sql = "UPDATE usuario SET activo = false WHERE id = ?")
@Where(clause = "activo = true")
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @Column(name = "bloqueado", nullable = false)
    private Boolean bloqueado = false;

    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos = 0;

    @Column(name = "token_reset_password")
    private String tokenResetPassword;

    @Column(name = "token_reset_password_expiracion")
    private LocalDateTime tokenResetPasswordExpiracion;

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