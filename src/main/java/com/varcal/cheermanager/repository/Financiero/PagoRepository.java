package com.varcal.cheermanager.repository.Financiero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

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

    // Suma pagada de VARIAS facturas en una sola consulta (evita una SUM por factura al listar).
    @Query("SELECT p.factura.id, COALESCE(SUM(p.monto), 0) FROM Pago p "
            + "WHERE p.estado = 'Pagado' AND p.factura.id IN :ids GROUP BY p.factura.id")
    List<Object[]> sumarPagadoPorFacturas(@Param("ids") Collection<Integer> ids);

    /** Mapa facturaId -> total pagado (las facturas sin pagos no aparecen: usa getOrDefault con cero). */
    default Map<Integer, BigDecimal> pagadoPorFacturas(Collection<Integer> ids) {
        Map<Integer, BigDecimal> resultado = new HashMap<>();
        List<Integer> lista = new ArrayList<>(new LinkedHashSet<>(ids));
        for (int i = 0; i < lista.size(); i += 1000) { // lotes de 1000 para no armar un IN gigante
            for (Object[] fila : sumarPagadoPorFacturas(lista.subList(i, Math.min(i + 1000, lista.size())))) {
                BigDecimal suma = fila[1] instanceof BigDecimal b ? b : new BigDecimal(fila[1].toString());
                resultado.put((Integer) fila[0], suma);
            }
        }
        return resultado;
    }

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