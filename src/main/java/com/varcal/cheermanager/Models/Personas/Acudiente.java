package com.varcal.cheermanager.Models.Personas;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Acudiente (madre, padre, tutor) de un deportista menor de edad. NO es un usuario del sistema:
 * es un contacto con responsabilidad legal. Un acudiente puede serlo de varios deportistas (hermanos).
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "acudientes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_acudiente_club_documento", columnNames = {"club_id", "numero_documento"})})
public class Acudiente {
    /** Club (tenant): Hibernate lo asigna solo al guardar y filtra por el toda consulta HQL/JPA. */
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "tipo_documento", length = 20)
    private String tipoDocumento;

    @Column(name = "numero_documento", length = 30)
    private String numeroDocumento;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "direccion", length = 200)
    private String direccion;

    /** Usuario con el que el acudiente entra al portal (null = sin acceso). */
    @Column(name = "usuario_id")
    private Integer usuarioId;
}
