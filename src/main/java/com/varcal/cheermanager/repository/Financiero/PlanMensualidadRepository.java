package com.varcal.cheermanager.repository.Financiero;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.varcal.cheermanager.Models.Financiero.PlanInscripcion;
import com.varcal.cheermanager.Models.Financiero.PlanMensualidad;

@Repository
public interface PlanMensualidadRepository extends JpaRepository<PlanMensualidad, Integer> {
    List<PlanMensualidad> findByActivoTrue();
}

