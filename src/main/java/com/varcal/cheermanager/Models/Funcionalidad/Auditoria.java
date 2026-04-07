package com.varcal.cheermanager.Models.Funcionalidad;

import java.time.LocalDateTime;

import com.varcal.cheermanager.Models.Auth.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "auditoria")
public class Auditoria {

    public enum AccionEnum {
        INSERT, UPDATE, DELETE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "tabla_afectada", nullable = false, length = 50)
    private String tablaAfectada;

    @Column(name = "accion", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccionEnum accion;

    @Column(name = "registro_id")
    private Integer registroId;

    @Column(name = "valores_anteriores", columnDefinition = "TEXT")
    private String valoresAnteriores;

    @Column(name = "nuevos_valores", columnDefinition = "TEXT")
    private String nuevosValores;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "ip_usuario", length = 45)
    private String ipUsuario;
}
