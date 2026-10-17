package com.varcal.cheermanager.Models.Clubes;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un club (cliente del SaaS). NO es una tabla de club (sin tenant): es la tabla raiz de la que cuelgan los demas.
 * Convencion: id 1 = club "Plataforma" (solo el superadmin); los clubes reales empiezan en 2.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "clubes")
public class Club {

    public static final int PLATAFORMA_ID = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    /** Identificador corto y unico (url / referencia): "ratels-all-stars". */
    @Column(name = "slug", nullable = false, unique = true, length = 60)
    private String slug;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
