package com.varcal.cheermanager.Models.Horario_Asistencia;

import java.time.LocalTime;

import com.varcal.cheermanager.Models.Org_dep.DiaSemana;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "horarios")
public class Horario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "dia_id", nullable = false)
    private DiaSemana dia;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;
}