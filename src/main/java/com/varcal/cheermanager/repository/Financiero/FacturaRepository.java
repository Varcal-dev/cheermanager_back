package com.varcal.cheermanager.repository.Financiero;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.varcal.cheermanager.Models.Financiero.Factura;

public interface FacturaRepository extends JpaRepository<Factura, Integer> {
    Optional<Factura> findByNumeroFactura(String numeroFactura);

    Optional<Factura> findByInscripcionId(Integer inscripcionId);

    long countByNumeroFacturaStartingWith(String prefijo);

    // Agregado nuevo para el dashboard de Inicio: total facturado del club
    // completo. El saldo pendiente por factura (total - pagado) se calcula
    // en el Service cruzando esto con PagoRepository.sumarMontoPagadoPorFactura,
    // para no duplicar esa lógica en una segunda query.
    @Query("SELECT COALESCE(SUM(f.total), 0) FROM Factura f")
    BigDecimal sumarTotalFacturado();
}