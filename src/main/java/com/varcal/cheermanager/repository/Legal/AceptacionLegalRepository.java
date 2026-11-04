package com.varcal.cheermanager.repository.Legal;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Legal.AceptacionLegal;

/** Filtrado por club automaticamente (@TenantId). */
public interface AceptacionLegalRepository extends JpaRepository<AceptacionLegal, Integer> {
    boolean existsByTipoAndVersion(String tipo, String version);

    List<AceptacionLegal> findAllByOrderByFechaDescIdDesc();
}
