package com.varcal.cheermanager.Models.Horario_Asistencia;

import java.time.LocalDate;

import com.varcal.cheermanager.Models.Personas.Deportista;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "asistencias")
public class Asistencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "deportista_id", nullable = false)
    private Deportista deportista;

    @ManyToOne
    @JoinColumn(name = "horario_entrenamiento_id", nullable = false)
    private HorarioEntrenamiento horarioEntrenamiento;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "estado_asistencia_id", nullable = false)
    private Integer estadoAsistenciaId;
}