package com.varcal.cheermanager.repository.Org_dep;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenador;

public interface GrupoEntrenadorRepository extends JpaRepository<GrupoEntrenador, Integer> {
    boolean existsByEntrenadorIdAndGrupoIdAndFechaFinIsNull(Integer entrenadorId, Integer grupoId);
}