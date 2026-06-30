package com.varcal.cheermanager.repository.Financiero;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.varcal.cheermanager.Models.Financiero.EstadoPago;
import com.varcal.cheermanager.Models.Financiero.Pago;

public interface PagoRepository extends JpaRepository<Pago, Integer> {

    List<Pago> findByFacturaId(Integer facturaId);

    List<Pago> findByEstado(EstadoPago estado);

    // Suma de los pagos de una factura que están efectivamente en estado
    // "Pagado". Se usa para saber cuánto le falta a la factura (saldo pendiente).
    // COALESCE evita que devuelva null cuando la factura no tiene pagos todavía.
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.factura.id = :facturaId AND p.estado = 'Pagado'")
    BigDecimal sumarMontoPagadoPorFactura(@Param("facturaId") Integer facturaId);

    // Listado de pagos de todas las facturas de una persona, útil para el
    // historial financiero de un deportista/entrenador.
    @Query("SELECT p FROM Pago p WHERE p.factura.persona.id = :personaId ORDER BY p.fecha DESC")
    List<Pago> findByPersonaId(@Param("personaId") Integer personaId);
}