package com.varcal.cheermanager.repository.Org_dep;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Org_dep.CategoriaNivel;

public interface CategoriaNivelRepository extends JpaRepository<CategoriaNivel, Integer> {

    List<CategoriaNivel> findByNivelId(Integer id);

}
