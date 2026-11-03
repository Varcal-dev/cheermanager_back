package com.varcal.cheermanager.repository.Clubes;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Clubes.SuscripcionPago;

public interface SuscripcionPagoRepository extends JpaRepository<SuscripcionPago, Integer> {
    List<SuscripcionPago> findByClubIdOrderByFechaPagoDescIdDesc(Integer clubId);
}
