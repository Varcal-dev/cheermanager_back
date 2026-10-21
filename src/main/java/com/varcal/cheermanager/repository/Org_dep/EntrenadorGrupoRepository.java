package com.varcal.cheermanager.repository.Org_dep;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.varcal.cheermanager.Models.Personas.Entrenador;

// La tabla puente `grupo_entrenador` (grupo_entrenamiento_id, entrenador_id)
// existe en el esquema SQL y el stored procedure sp_asignar_entrenador_grupo
// escribe en ella, pero no tenía ningún Model/Repository JPA — no había
// forma de leer "qué grupos tiene a cargo un entrenador" desde el código.
// Se usa Repository<Entrenador, Integer> (marcador, sin los métodos CRUD de
// JpaRepository) en vez de un @Entity con clave compuesta, porque la tabla
// no tiene PK propia de un solo campo y no se necesita más que leer.
public interface EntrenadorGrupoRepository extends Repository<Entrenador, Integer> {

    @Query(value = "SELECT grupo_entrenamiento_id FROM grupo_entrenador WHERE entrenador_id = :entrenadorId AND club_id = :#{T(com.varcal.cheermanager.tenant.TenantContext).require()}", nativeQuery = true)
    List<Integer> findGrupoIdsByEntrenadorId(@Param("entrenadorId") Integer entrenadorId);

    @Query(value = "SELECT entrenador_id FROM grupo_entrenador WHERE grupo_entrenamiento_id = :grupoId AND club_id = :#{T(com.varcal.cheermanager.tenant.TenantContext).require()}", nativeQuery = true)
    List<Integer> findEntrenadorIdsByGrupoId(@Param("grupoId") Integer grupoId);

    @Query(value = "SELECT COUNT(*) FROM grupo_entrenador WHERE entrenador_id = :entrenadorId AND club_id = :#{T(com.varcal.cheermanager.tenant.TenantContext).require()}", nativeQuery = true)
    long countGruposByEntrenadorId(@Param("entrenadorId") Integer entrenadorId);
}