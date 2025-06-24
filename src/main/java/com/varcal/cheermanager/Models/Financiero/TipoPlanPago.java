package com.varcal.cheermanager.Models.Financiero;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "tipos_planes_pago")
public class TipoPlanPago {
    
    public enum Categoria {
        inscripcion, mensualidad
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nombre;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categoria categoria;
    
    @Column(name = "duracion_meses", nullable = false)
    private Integer duracionMeses;
    
    @Column(columnDefinition = "TEXT")
    private String descripcion;
}
