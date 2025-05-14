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

    @ManyToOne
    @JoinColumn(name = "tipo_grupo_id")
    private TipoGrupo tipoGrupo;

    @ManyToOne
    @JoinColumn(name = "categorias_nivel_id")
    private CategoriaNivel categoriaNivel;
}