package com.varcal.cheermanager.Models.Personas;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Habilidad del glosario que el deportista dice tener al registrarse (declaracion, no validacion). */
@Data
@NoArgsConstructor
@Entity
@Table(name = "ficha_habilidades_declaradas")
public class FichaHabilidadDeclarada {
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

    @Column(name = "habilidad_id", nullable = false)
    private Integer habilidadId;
}
