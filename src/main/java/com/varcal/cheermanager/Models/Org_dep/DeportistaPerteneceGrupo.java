package com.varcal.cheermanager.Models.Org_dep;

import java.time.LocalDate;

import com.varcal.cheermanager.Models.Personas.Deportista;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "deportista_pertenece_grupo")
public class DeportistaPerteneceGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "grupo_id")
    private GrupoEntrenamiento grupo;

    @ManyToOne
    @JoinColumn(name = "deportista_id")
    private Deportista deportista;

    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String observaciones;
}
