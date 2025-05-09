package com.varcal.cheermanager.Models.Evaluaciones;

import java.time.LocalDate;

import com.varcal.cheermanager.Models.Personas.Deportista;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "objetivos")
public class Objetivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "deportista_id", nullable = false)
    private Deportista deportista;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDate fechaCreacion;

    @Column(name = "estado_objetivo_id", nullable = false)
    private Integer estadoObjetivoId;
}