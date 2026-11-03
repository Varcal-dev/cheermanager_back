package com.varcal.cheermanager.repository.Clubes;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Clubes.PlanSaas;

public interface PlanSaasRepository extends JpaRepository<PlanSaas, Integer> {
    Optional<PlanSaas> findByCodigo(String codigo);

    List<PlanSaas> findByActivoTrueOrderByOrdenAsc();
}
