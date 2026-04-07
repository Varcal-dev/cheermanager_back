package com.varcal.cheermanager.Models.Personas;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "historial_estado_deportista")
public class HistorialDeportistaEstado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "deportista_id", nullable = false)
    private Deportista deportista;

    @ManyToOne
    @JoinColumn(name = "estado_anterior_id")
    private EstadoPersona estadoAnterior;

    @ManyToOne
    @JoinColumn(name = "estado_nuevo_id", nullable = false)
    private EstadoPersona estadoNuevo;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio;

    @Column(name = "usuario_id")
    private Integer usuarioId;

    @Column(name = "motivo_cambio")
    private String motivoCambio;
}
