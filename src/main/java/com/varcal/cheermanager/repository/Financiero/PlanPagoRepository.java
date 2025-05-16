package com.varcal.cheermanager.repository.Financiero;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.varcal.cheermanager.Models.Financiero.PlanPago;

@Repository
public interface PlanPagoRepository extends JpaRepository<PlanPago, Integer> {
    List<PlanPago> findByActivoTrue();
}

