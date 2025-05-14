package com.varcal.cheermanager.repository.Org_dep;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Org_dep.Division;

public interface DivisionRepository extends JpaRepository<Division, Integer> {
    // Custom query methods can be defined here if needed
    // For example:
    // List<Division> findBySomeField(String someField);

}
