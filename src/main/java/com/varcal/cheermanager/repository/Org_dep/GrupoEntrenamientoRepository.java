package com.varcal.cheermanager.repository.Org_dep;

import java.sql.Date;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;

public interface GrupoEntrenamientoRepository extends JpaRepository<GrupoEntrenamiento, Integer> {
        // (la verificacion de elegibilidad se calcula en GrupoEntrenamientoService; ya no hay funcion de BD)
}