package com.varcal.cheermanager.Models.Org_dep;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "categorias_nivel")
public class CategoriaNivel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "nivel_id", nullable = false)
    private Nivel nivel;

    @ManyToOne
    @JoinColumn(name = "division_id", nullable = false)
    private Division division;

    @ManyToOne
    @JoinColumn(name = "estado_categorias_nivel_id", nullable = false)
    private EstadoCategoriaNivel estadoCategoriaNivel;

    // --- Agregado: estas 4 columnas ya existían en la tabla (el seed V4 las
    // llena con los datos del PDF FEDECOLCHEER 2026) pero no estaban
    // mapeadas acá — antes eran invisibles para cualquier código Java,
    // incluido el cruce por división que se usa en el dashboard de Personas.

    @Column(name = "año_nacimiento_min")
    private Integer añoNacimientoMin; // null = sin mínimo (ej. divisiones "Open": "Hasta 2011")

    @Column(name = "año_nacimiento_max")
    private Integer añoNacimientoMax;

    @Column(name = "cantidad_min")
    private Integer cantidadMin;

    @Column(name = "cantidad_max")
    private Integer cantidadMax;

    // NOTA (heredada del comentario del seed): la tabla oficial también
    // exige límites de CANTIDAD DE HOMBRES para "Open Medium" (máx. 8) y
    // "Open Large" (mín. 9) dentro de Elite 4. No hay columna para esto
    // todavía (cantidad_hombres_min / cantidad_hombres_max) — si se necesita
    // validar esa regla en código, hay que agregarla a la tabla y acá.
}