package com.varcal.cheermanager.repository.Financiero;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.varcal.cheermanager.Models.Financiero.Factura;

public interface FacturaRepository extends JpaRepository<Factura, Integer> {
    Optional<Factura> findByNumeroFactura(String numeroFactura);

    // Antes era Optional<Factura> (una sola). Cambió a List porque ahora una
    // inscripción puede tener varias facturas: la de inscripción (periodo
    // null) más una por cada mes de mensualidad ya facturado (periodo
    // yyyy-MM). Ver InscripcionService para los 3 lugares que dependían del
    // supuesto de "una sola factura por inscripción".
    List<Factura> findByInscripcionId(Integer inscripcionId);

    // Para no facturar el mismo mes dos veces: antes de generar el cobro de
    // un período, se verifica si ya existe una factura de esta inscripción
    // para ese mismo período.
    Optional<Factura> findByInscripcionIdAndPeriodo(Integer inscripcionId, String periodo);

    long countByNumeroFacturaStartingWith(String prefijo);

    // Agregado nuevo para el dashboard de Inicio: total facturado del club
    // completo. El saldo pendiente por factura (total - pagado) se calcula
    // en el Service cruzando esto con PagoRepository.sumarMontoPagadoPorFactura,
    // para no duplicar esa lógica en una segunda query.
    @Query("SELECT COALESCE(SUM(f.total), 0) FROM Factura f")
    BigDecimal sumarTotalFacturado();
}