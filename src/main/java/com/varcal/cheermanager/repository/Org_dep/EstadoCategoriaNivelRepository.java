package com.varcal.cheermanager.repository.Org_dep;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Org_dep.EstadoCategoriaNivel;

public interface EstadoCategoriaNivelRepository extends JpaRepository<EstadoCategoriaNivel, Integer> {
    // Custom query methods can be defined here if needed
    // For example:
    // List<EstadoCategoriaNivel> findBySomeField(String someField);

}
