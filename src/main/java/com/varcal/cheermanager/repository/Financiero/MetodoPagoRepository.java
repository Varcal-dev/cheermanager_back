package com.varcal.cheermanager.repository.Financiero;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Financiero.MetodoPago;

public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Integer> {
}