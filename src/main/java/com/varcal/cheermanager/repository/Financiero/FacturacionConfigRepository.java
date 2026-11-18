package com.varcal.cheermanager.repository.Financiero;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Financiero.FacturacionConfig;

public interface FacturacionConfigRepository extends JpaRepository<FacturacionConfig, Integer> {
    /** Hibernate filtra por el club actual: hay a lo sumo una fila. */
    Optional<FacturacionConfig> findFirstByOrderByIdAsc();
}
