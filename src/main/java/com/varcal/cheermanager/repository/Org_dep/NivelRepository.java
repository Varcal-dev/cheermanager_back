package com.varcal.cheermanager.repository.Org_dep;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Org_dep.Nivel;

public interface NivelRepository extends JpaRepository<Nivel, Integer> {
    // Custom query methods can be defined here if needed
    // For example:
    // List<Nivel> findBySomeField(String someField);

}
