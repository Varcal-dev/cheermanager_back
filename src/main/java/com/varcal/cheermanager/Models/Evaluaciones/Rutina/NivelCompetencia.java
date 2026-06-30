package com.varcal.cheermanager.Models.Evaluaciones.Rutina;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

// Catálogo de niveles de competencia según el United Scoring System
// (FEDECOLCHEER/IASF). Hoy: N1, N2, N3, N4, N4.2 (modalidad Élite). A futuro:
// niveles de Prep (1.1, 2.1) y Novice (Tiny/Mini/Youth), que usan la MISMA
// estructura de configuración (TablaCantidadNivel + TopeSeccionNivel) pero con
// sus propios valores — no requiere cambios de modelo, solo cargar filas nuevas.
@Data
@NoArgsConstructor
@Entity
@Table(name = "niveles_competencia")
public class NivelCompetencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Ej: "N1", "N2", "N3", "N4", "N4.2", "Prep 1.1", "Novice Tiny"
    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;

    // Ej: "Elite", "Prep", "Novice" — agrupa niveles de la misma modalidad.
    @Column(name = "modalidad", nullable = false)
    private String modalidad;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}