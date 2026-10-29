package com.varcal.cheermanager.repository.Financiero;

import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.varcal.cheermanager.Models.Financiero.Factura;

public interface FacturaRepository extends JpaRepository<Factura, Integer> {

    // Bloqueo pesimista (SELECT ... FOR UPDATE): serializa los pagos concurrentes sobre la
    // MISMA factura. Sin esto, dos pagos simultaneos leen el mismo saldo y ambos pasan la
    // validacion de sobrepago.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Factura f WHERE f.id = :id")
    Optional<Factura> findByIdForUpdate(@Param("id") Integer id);
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
    @Query("SELECT COALESCE(SUM(f.total), 0) FROM Factura f WHERE f.anulada = false")
    BigDecimal sumarTotalFacturado();

    // Facturas vigentes (las anuladas no cuentan en cartera ni en los dashboards).
    List<Factura> findByAnuladaFalse();

    // Listado completo trayendo persona y descuento en la MISMA consulta (evita una consulta por factura).
    @Query("SELECT f FROM Factura f JOIN FETCH f.persona LEFT JOIN FETCH f.descuento")
    List<Factura> findAllConDetalle();

    List<Factura> findByPersonaIdOrderByFechaEmisionDesc(Integer personaId);

    // Cartera en mora: vigentes, ya vencidas (fecha_vencimiento < hoy) y con saldo > 0.
    @Query("SELECT f FROM Factura f WHERE f.anulada = false AND f.fechaVencimiento < :hoy "
            + "AND f.total > (SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.factura = f AND p.estado = 'Pagado') "
            + "ORDER BY f.fechaVencimiento ASC")
    List<Factura> findEnMora(@Param("hoy") LocalDate hoy);


    // Personas que ya tienen un cobro VIGENTE de este evento (para no cobrar dos veces en lote).
    @Query("SELECT DISTINCT f.persona.id FROM Factura f WHERE f.eventoId = :eventoId "
            + "AND f.concepto = 'EVENTO' AND f.anulada = false")
    List<Integer> findPersonaIdsConCobroDeEvento(@Param("eventoId") Integer eventoId);

    // ---- Listado paginado con filtros ------------------------------------------------
    // El estado de cuenta no se guarda: se deriva de los pagos. Por eso el filtro por estado usa la
    // misma suma de pagos 'Pagado' como subconsulta, con las MISMAS reglas que FacturaService.construirDTO:
    //   ANULADA > PAGADA (saldo<=0) > EN_MORA (vencida con saldo) > ABONO_PARCIAL > PENDIENTE.
    // Los parametros nunca van null (el Service pone valores por defecto) para evitar problemas
    // de tipado de nulos en Hibernate 6 / MySQL.
    String PAGADO_SQ = "(SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.factura = f AND p.estado = 'Pagado')";

    String FILTRO_PAGINA = " WHERE f.fechaEmision BETWEEN :desde AND :hasta "
            + "AND (:q = '' OR LOWER(f.numeroFactura) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "     OR LOWER(f.descripcion) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "     OR LOWER(CONCAT(pe.nombre, ' ', pe.apellidos)) LIKE LOWER(CONCAT('%', :q, '%'))) "
            + "AND (:concepto = 'TODOS' OR f.concepto = :concepto) "
            + "AND (:estado = 'TODOS' "
            + "  OR (:estado = 'ANULADA' AND f.anulada = true) "
            + "  OR (f.anulada = false AND ( "
            + "       (:estado = 'PAGADA' AND f.total <= " + PAGADO_SQ + ") "
            + "    OR (:estado = 'EN_MORA' AND f.total > " + PAGADO_SQ + " AND f.fechaVencimiento < :hoy) "
            + "    OR (:estado = 'ABONO_PARCIAL' AND f.total > " + PAGADO_SQ + " AND " + PAGADO_SQ + " > 0 "
            + "        AND (f.fechaVencimiento IS NULL OR f.fechaVencimiento >= :hoy)) "
            + "    OR (:estado = 'PENDIENTE' AND f.total > " + PAGADO_SQ + " AND " + PAGADO_SQ + " = 0 "
            + "        AND (f.fechaVencimiento IS NULL OR f.fechaVencimiento >= :hoy)) "
            + "  ))) ";

    @Query(value = "SELECT f FROM Factura f JOIN FETCH f.persona pe LEFT JOIN FETCH f.descuento" + FILTRO_PAGINA
            + "ORDER BY f.fechaEmision DESC, f.id DESC",
           countQuery = "SELECT COUNT(f) FROM Factura f JOIN f.persona pe" + FILTRO_PAGINA)
    Page<Factura> buscarPagina(@Param("q") String q,
                               @Param("estado") String estado,
                               @Param("concepto") String concepto,
                               @Param("desde") LocalDate desde,
                               @Param("hasta") LocalDate hasta,
                               @Param("hoy") LocalDate hoy,
                               Pageable pageable);
}
