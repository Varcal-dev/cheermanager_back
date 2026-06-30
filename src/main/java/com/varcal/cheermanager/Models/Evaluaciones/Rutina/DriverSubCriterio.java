package com.varcal.cheermanager.Models.Evaluaciones.Rutina;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

// Catálogo de "drivers" adicionales que se suman al escalón base de un
// sub-criterio: Grado de Dificultad (cada habilidad suma 0.1 o 0.2 según sea
// Avanzada o Élite) y Participación Máxima (0.3/0.5/0.7 según el nivel de
// dificultad alcanzado por el grupo MÁXIMO). Ver TopeDriverNivel para el
// tope configurable de cada uno por nivel.
@Data
@NoArgsConstructor
@Entity
@Table(name = "drivers_sub_criterio")
public class DriverSubCriterio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "sub_criterio_id", nullable = false)
    private SubCriterioRubrica subCriterio;

    // "Grado de Dificultad" | "Participacion Maxima"
    @Column(name = "nombre", nullable = false)
    private String nombre;
}