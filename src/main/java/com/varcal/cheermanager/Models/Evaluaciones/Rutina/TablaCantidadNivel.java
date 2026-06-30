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

// Una fila de las "TABLA DE CANTIDAD EN CONSTRUCCIÓN" / "TABLA DE CANTIDAD EN
// SALTOS/GIMNASIA" de la rúbrica. Ej: para N1, sección "Construcciones",
// rango 5-11 atletas: mayoria=1, granParte=2, maximo=3 (número de grupos).
// Para N1, sección "Saltos/Gimnasia", rango 5-11 atletas: mayoria=5,
// granParte=6, maximo=10 (número de atletas, no de grupos — la unidad varía
// según la sección, ver `unidad`).
//
// Esta tabla es EDITABLE desde el sistema (a diferencia de las fórmulas de
// cálculo, que viven en código) porque la rúbrica oficial cambia estos
// números de temporada a temporada sin cambiar la metodología de fondo.
@Data
@NoArgsConstructor
@Entity
@Table(name = "tabla_cantidad_nivel")
public class TablaCantidadNivel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "nivel_id", nullable = false)
    private NivelCompetencia nivel;

    // "Construcciones" (cuenta grupos) o "Saltos_Gimnasia" (cuenta atletas).
    // Ver campo `unidad` para saber qué representan mayoria/granParte/maximo
    // en cada fila.
    @Column(name = "tabla", nullable = false)
    private String tabla;

    // "grupos" o "atletas" — aclara qué unidad usan mayoria/granParte/maximo
    // en esta fila, porque la rúbrica usa la misma estructura de 3 columnas
    // para ambos casos con significado distinto.
    @Column(name = "unidad", nullable = false)
    private String unidad;

    @Column(name = "rango_min_atletas", nullable = false)
    private Integer rangoMinAtletas;

    @Column(name = "rango_max_atletas", nullable = false)
    private Integer rangoMaxAtletas;

    @Column(name = "mayoria", nullable = false)
    private Integer mayoria;

    @Column(name = "gran_parte", nullable = false)
    private Integer granParte;

    @Column(name = "maximo", nullable = false)
    private Integer maximo;
}