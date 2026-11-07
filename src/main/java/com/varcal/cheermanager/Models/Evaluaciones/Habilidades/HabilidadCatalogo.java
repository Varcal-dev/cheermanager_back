package com.varcal.cheermanager.Models.Evaluaciones.Habilidades;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Habilidad del glosario N1-N4 (catalogo GLOBAL, igual para todos los clubes). Cada fila es una habilidad distinta
 * ("cada habilidad separada por una viñeta se considera una habilidad diferente"). La categoria decide que drivers
 * se aplican al evaluarla (ver DriversHabilidad).
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "habilidades_catalogo")
public class HabilidadCatalogo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "codigo", nullable = false, unique = true, length = 40)
    private String codigo;

    /** N1 | N2 | N3 | N4 */
    @Column(name = "nivel", nullable = false, length = 10)
    private String nivel;

    /** ELEVACION | GIMNASIA_ESTATICA | GIMNASIA_CARRERA | LANZAMIENTO */
    @Column(name = "categoria", nullable = false, length = 30)
    private String categoria;

    /** Elevaciones: INVERSION, SUELTO, GIRO, COMBINACION, COED, DESMONTE. Lanzamientos: SIN_GIRO, CON_GIRO. */
    @Column(name = "estilo", length = 20)
    private String estilo;

    /** APROPIADA | AVANZADA | ELITE (los lanzamientos no tienen escalon). */
    @Column(name = "tier", length = 12)
    private String tier;

    @Column(name = "nombre", nullable = false, length = 300)
    private String nombre;

    @Column(name = "nombre_en", length = 300)
    private String nombreEn;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
