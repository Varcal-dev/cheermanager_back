package com.varcal.cheermanager.Models.Org_dep;

    import jakarta.persistence.*;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    @Data
    @NoArgsConstructor
    @Entity
    @Table(name = "estados_categorias_nivel") // Asume un nombre de tabla apropiado
    public class EstadoCategoriaNivel {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        @Column(name = "estado_categoria", nullable = false)
        private String nombre;

        // Getters y setters (Lombok se encarga con @Data)
    }
