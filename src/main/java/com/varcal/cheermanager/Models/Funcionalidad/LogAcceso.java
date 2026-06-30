package com.varcal.cheermanager.Models.Funcionalidad;

import java.time.LocalDateTime;

import com.varcal.cheermanager.Models.Auth.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "log_acceso")
@Data
public class LogAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Opcional: en un intento de login con email que no existe, no hay Usuario
    // al que enlazar, pero igual vale la pena registrar el intento (por eso
    // nullable = true y se guarda también el email tal cual se recibió).
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = true)
    private Usuario usuario;

    @Column(name = "email_intento", nullable = false)
    private String emailIntento;

    @Column(nullable = false)
    private LocalDateTime fecha;

    // LOGIN_EXITOSO | LOGIN_FALLIDO | LOGOUT | CUENTA_BLOQUEADA
    @Column(nullable = false, length = 30)
    private String accion;

    @Column(name = "ip_origen", length = 45)
    private String ipOrigen;

    // Detalle adicional opcional (ej. "invalid_password", "account_locked")
    @Column(length = 255)
    private String detalle;
}