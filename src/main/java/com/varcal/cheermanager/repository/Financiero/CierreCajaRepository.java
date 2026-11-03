package com.varcal.cheermanager.repository.Financiero;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Financiero.CierreCaja;

public interface CierreCajaRepository extends JpaRepository<CierreCaja, Integer> {
    Optional<CierreCaja> findByFecha(LocalDate fecha);

    List<CierreCaja> findByFechaBetweenOrderByFechaDesc(LocalDate desde, LocalDate hasta);
}
