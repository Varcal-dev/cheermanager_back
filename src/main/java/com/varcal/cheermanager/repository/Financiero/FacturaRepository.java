package com.varcal.cheermanager.repository.Financiero;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Financiero.Factura;

public interface FacturaRepository extends JpaRepository<Factura, Integer> {
    Optional<Factura> findByNumeroFactura(String numeroFactura);

    // Permite ubicar la factura que se generó automáticamente al crear una
    // inscripción (una inscripción tiene a lo sumo una factura "de origen").
    Optional<Factura> findByInscripcionId(Integer inscripcionId);

    long countByNumeroFacturaStartingWith(String prefijo);
}