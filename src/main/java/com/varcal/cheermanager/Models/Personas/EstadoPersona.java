package com.varcal.cheermanager.Models.Personas;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@Table(name = "estados_persona")
public class EstadoPersona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "estado", nullable = false)
    private String estado;

    
}
