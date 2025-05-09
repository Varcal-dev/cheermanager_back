package com.varcal.cheermanager.Models.Horario_Asistencia;

import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "horarios_entrenamiento")
public class HorarioEntrenamiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "horario_id", nullable = false)
    private Horario horario;

    @ManyToOne
    @JoinColumn(name = "grupo_entrenamiento_id", nullable = false)
    private GrupoEntrenamiento grupoEntrenamiento;
}