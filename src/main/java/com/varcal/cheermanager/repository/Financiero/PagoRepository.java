package com.varcal.cheermanager.repository.Financiero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.varcal.cheermanager.Models.Financiero.EstadoPago;
import com.varcal.cheermanager.Models.Financiero.Pago;

public interface PagoRepository extends JpaRepository<Pago, Integer> {

    List<Pago> findByFacturaId(Integer facturaId);

    List<Pago> findByEstado(EstadoPago estado);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.factura.id = :facturaId AND p.estado = 'Pagado'")
    BigDecimal sumarMontoPagadoPorFactura(@Param("facturaId") Integer facturaId);

    @Query("SELECT p FROM Pago p WHERE p.factura.persona.id = :personaId ORDER BY p.fecha DESC")
    List<Pago> findByPersonaId(@Param("personaId") Integer personaId);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.estado = 'Pagado'")
    BigDecimal sumarTotalRecaudado();

    // Nuevo: recaudo dentro de un rango de fechas — base del KPI de
    // "recaudo del mes" y de la tendencia de los últimos meses en el
    // dashboard de Finanzas.
    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.estado = 'Pagado' AND p.fecha BETWEEN :desde AND :hasta")
    BigDecimal sumarMontoPagadoEnRango(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}