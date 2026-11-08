package com.varcal.cheermanager.Models.Personas;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Experiencia previa y tallas de un deportista (una fila por deportista). */
@Data
@NoArgsConstructor
@Entity
@Table(name = "fichas_deportivas")
public class FichaDeportiva {
    /** Club (tenant): Hibernate lo asigna solo al guardar y filtra por el toda consulta HQL/JPA. */
    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "deportista_id", nullable = false)
    private Integer deportistaId;

    /** null = sin responder. */
    @Column(name = "tiene_experiencia")
    private Boolean tieneExperiencia;

    @Column(name = "anios_experiencia")
    private Integer aniosExperiencia;

    /** Nivel del glosario (N1-N4) que el deportista cree tener. */
    @Column(name = "nivel_declarado", length = 10)
    private String nivelDeclarado;

    @Column(name = "talla_uniforme", length = 10)
    private String tallaUniforme;

    @Column(name = "talla_falda_short", length = 10)
    private String tallaFaldaShort;

    @Column(name = "talla_camiseta", length = 10)
    private String tallaCamiseta;

    @Column(name = "talla_buzo", length = 10)
    private String tallaBuzo;

    @Column(name = "talla_calzado", length = 10)
    private String tallaCalzado;
}
