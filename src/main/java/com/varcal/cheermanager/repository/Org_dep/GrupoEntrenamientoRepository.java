package com.varcal.cheermanager.repository.Org_dep;

import java.sql.Date;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;

public interface GrupoEntrenamientoRepository extends JpaRepository<GrupoEntrenamiento, Integer> {
        @Query(value = "SELECT verificar_elegibilidad_deportista_grupo(:deportistaId, :grupoId) AS resultado", nativeQuery = true)
        String verificarElegibilidadDeportista(@Param("deportistaId") Integer deportistaId,
                        @Param("grupoId") Integer grupoId);

        @Procedure(procedureName = "agregar_deportista_a_grupo")
        String agregarDeportistaAGrupo(
                        @Param("p_deportista_id") Integer deportistaId,
                        @Param("p_grupo_id") Integer grupoId,
                        @Param("p_observaciones") String observaciones);

        @Procedure(procedureName = "sp_asignar_entrenador_grupo")
        String asignarEntrenadorAGrupo(
                        @Param("p_entrenador_id") Integer entrenadorId,
                        @Param("p_grupo_id") Integer grupoId,
                        @Param("p_fecha_inicio") Date fechaInicio,
                        @Param("p_fecha_fin") Date fechaFin,
                        @Param("p_rol") String rol);
}
