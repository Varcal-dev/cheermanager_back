package com.varcal.cheermanager.Models.Org_dep;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "grupos_entrenamiento")
public class GrupoEntrenamiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(name = "nombre", nullable = false)
    private String nombre;
    @Column(name = "tipo_grupo_id")
    private Integer tipoGrupoId;
    @ManyToOne
    @JoinColumn(name = "categorias_nivel_id", nullable = false)
    private CategoriaNivel categoriaNivel;
}