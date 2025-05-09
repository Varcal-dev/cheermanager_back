package com.varcal.cheermanager.Models.Eventos;

import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "grupos_evento")
public class GrupoEvento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "grupo_entrenamiento_id", nullable = false)
    private GrupoEntrenamiento grupoEntrenamiento;

    @ManyToOne
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;
}