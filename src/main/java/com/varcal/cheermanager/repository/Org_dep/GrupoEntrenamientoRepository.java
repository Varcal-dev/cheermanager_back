package com.varcal.cheermanager.repository.Org_dep;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;

public interface GrupoEntrenamientoRepository extends JpaRepository<GrupoEntrenamiento, Integer> {
    @Query(value = "SELECT verificar_elegibilidad_deportista_grupo(:deportistaId, :grupoId) AS resultado", nativeQuery = true)
    String verificarElegibilidadDeportista(@Param("deportistaId") Integer deportistaId, @Param("grupoId") Integer grupoId);

}
