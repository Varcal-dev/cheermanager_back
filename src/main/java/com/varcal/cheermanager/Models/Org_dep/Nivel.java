package com.varcal.cheermanager.Models.Org_dep;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "niveles", uniqueConstraints = {@jakarta.persistence.UniqueConstraint(name = "uk_nivel_club_nombre", columnNames = {"club_id", "nombre"})})
public class Nivel {
    /** Club (tenant): Hibernate lo asigna solo al guardar y filtra por el toda consulta HQL/JPA. */
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false)
    @NotBlank(message = "El nombre del nivel es obligatorio")
    @Size(max = 255, message = "El nombre del nivel no puede superar 255 caracteres")
    private String nombre;
}