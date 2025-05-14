package com.varcal.cheermanager.repository.Org_dep;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.varcal.cheermanager.Models.Org_dep.ReglaCategoria;

@Repository
public interface ReglaCategoriaRepository extends JpaRepository<ReglaCategoria, Integer> {

    ReglaCategoria findByCategoriaNivelId(Integer id);
}

