package com.varcal.cheermanager.repository.Financiero;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Financiero.TipoPlanPago;

public interface TipoPlanPagoRepository extends JpaRepository<TipoPlanPago, Integer> {
    // Usado por el selector de "tipo de plan" en el formulario de creación,
    // filtrado según si se está creando un plan de inscripción o de
    // mensualidad (evita que aparezcan mezclados los tipos de la otra
    // categoría).
    List<TipoPlanPago> findByCategoria(TipoPlanPago.Categoria categoria);
}