package com.varcal.cheermanager.Models.Org_dep;

import java.time.LocalDate;

import com.varcal.cheermanager.Models.Personas.Entrenador;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "grupo_entrenador")
public class GrupoEntrenador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "grupo_entrenamiento_id")
    private GrupoEntrenamiento grupo;

    @ManyToOne
    @JoinColumn(name = "entrenador_id")
    private Entrenador entrenador;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    private String rol;
}