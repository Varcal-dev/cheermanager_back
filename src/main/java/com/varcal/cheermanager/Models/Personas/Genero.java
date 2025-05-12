package com.varcal.cheermanager.Models.Personas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

// Genero.java
@Entity
@Data
@NoArgsConstructor
@Table(name = "generos")
public class Genero {
    @Id
    private Integer id;

    @Column(nullable = false)
    private String genero;

    // Getters y setters
}