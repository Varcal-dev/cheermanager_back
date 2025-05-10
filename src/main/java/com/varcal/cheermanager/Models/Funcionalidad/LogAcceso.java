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

@Entity
@Table(name = "logs_acceso")
public class LogAcceso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    
    @Column(name = "direccion_ip", nullable = false, length = 45)
    private String direccionIp;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "exito", nullable = false)
    private Boolean exito = true;
    
    @Column(name = "mensaje_error")
    private String mensajeError;
    
    @Column(name = "fecha_acceso", nullable = false)
    private LocalDateTime fechaAcceso = LocalDateTime.now();
    
    // Getters, setters, constructores
}
