package com.varcal.cheermanager.Models.Horario_Asistencia;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Espacio dentro de una sede (colchoneta, salon...). Define cuantos grupos pueden usarlo a la vez. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "espacios")
public class Espacio {

    public enum Tipo { COLCHONETA, GIMNASIO, SALON, CANCHA, EXTERIOR, OTRO }

    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "sede_id", nullable = false) private Integer sedeId;
    @Column(name = "nombre", nullable = false, length = 100) private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private Tipo tipo;

    /** Deportistas que caben (informativo: avisa, no bloquea). */
    @Column(name = "capacidad") private Integer capacidad;

    /** Cuantos grupos pueden usar el espacio al mismo tiempo (una colchoneta grande puede ser 2). */
    @Column(name = "grupos_simultaneos", nullable = false) private int gruposSimultaneos = 1;

    @Column(name = "activo", nullable = false) private boolean activo = true;
}
