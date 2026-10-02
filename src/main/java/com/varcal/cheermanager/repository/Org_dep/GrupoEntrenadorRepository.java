package com.varcal.cheermanager.repository.Org_dep;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenador;

public interface GrupoEntrenadorRepository extends JpaRepository<GrupoEntrenador, Integer> {
    boolean existsByEntrenadorIdAndGrupoIdAndFechaFinIsNull(Integer entrenadorId, Integer grupoId);

    // Historial completo (activas e históricas) de asignaciones de un
    // entrenador a grupos — lo usa el panel "Historial de Asignación a
    // Grupos" en entrenadores/page.tsx.
    List<GrupoEntrenador> findByEntrenadorIdOrderByFechaInicioDesc(Integer entrenadorId);
}