package com.varcal.cheermanager.repository.Financiero;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Financiero.Factura;

public interface FacturaRepository extends JpaRepository<Factura, Integer> {
    Optional<Factura> findByNumeroFactura(String numeroFactura);
}
