package com.varcal.cheermanager.repository.Financiero;

import com.varcal.cheermanager.Models.Financiero.PlanInscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanInscripcionRepository extends JpaRepository<PlanInscripcion, Integer> {
}
