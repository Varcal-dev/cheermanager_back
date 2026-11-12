package com.varcal.cheermanager.Service.Reportes;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.varcal.cheermanager.tenant.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Reporte financiero consolidado del club: facturado y cobrado por mes, por concepto y por metodo de pago,
 * y la cartera pendiente HOY con su antiguedad. Solo lectura. Consultas nativas filtradas a mano por club_id.
 *
 * Criterios (los mismos de la pantalla de facturas): una factura anulada no cuenta; solo los pagos en estado
 * "Pagado" cuentan como cobrado; una factura esta en mora si tiene saldo y su fecha de vencimiento ya paso.
 */
@Service
public class ReportesService {

    /** Rango maximo de un reporte o exporte: evita consultas gigantes por error. */
    public static final int MAX_DIAS_RANGO = 366 * 5;

    public record Rango(LocalDate desde, LocalDate hasta) {
    }

    public record Mes(String mes, BigDecimal facturado, BigDecimal cobrado) {
    }

    public record Item(String nombre, BigDecimal monto, long cantidad) {
    }

    public record Tramo(String etiqueta, long facturas, BigDecimal saldo) {
    }

    public record Cartera(long facturasConSaldo, BigDecimal saldoPendiente, long facturasEnMora,
            BigDecimal saldoEnMora, List<Tramo> antiguedad) {
    }

    public record Totales(BigDecimal facturado, long facturas, BigDecimal cobrado, long pagos) {
    }

    public record Financiero(LocalDate desde, LocalDate hasta, Totales totales, List<Mes> meses,
            List<Item> porConcepto, List<Item> porMetodo, Cartera cartera) {
    }

    @PersistenceContext
    private EntityManager em;

    /** Normaliza el rango: por defecto los ultimos 12 meses; valida orden y tamano. */
    public static Rango rango(LocalDate desde, LocalDate hasta, LocalDate hoy) {
        LocalDate h = hasta != null ? hasta : hoy;
        LocalDate d = desde != null ? desde : h.minusYears(1).plusDays(1);
        if (d.isAfter(h)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha inicial no puede ser posterior a la final.");
        }
        if (d.plusDays(MAX_DIAS_RANGO).isBefore(h)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rango máximo es de 5 años.");
        }
        return new Rango(d, h);
    }

    @Transactional(readOnly = true)
    public Financiero financiero(LocalDate desde, LocalDate hasta, LocalDate hoy) {
        Rango r = rango(desde, hasta, hoy);
        Integer club = TenantContext.require();

        // Meses del rango, aunque no tengan movimiento (las graficas no deben saltarse meses).
        Map<String, BigDecimal[]> meses = new TreeMap<>();
        for (YearMonth m = YearMonth.from(r.desde()); !m.isAfter(YearMonth.from(r.hasta())); m = m.plusMonths(1)) {
            meses.put(m.toString(), new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
        }

        BigDecimal totalFacturado = BigDecimal.ZERO;
        long nFacturas = 0;
        for (Object[] f : consulta("SELECT DATE_FORMAT(f.fecha_emision, '%Y-%m'), SUM(f.total), COUNT(*) FROM facturas f "
                + "WHERE f.club_id = :club AND f.anulada = 0 AND f.fecha_emision BETWEEN :desde AND :hasta GROUP BY 1",
                club, r)) {
            BigDecimal monto = dec(f[1]);
            meses.computeIfAbsent((String) f[0], k -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO })[0] = monto;
            totalFacturado = totalFacturado.add(monto);
            nFacturas += ((Number) f[2]).longValue();
        }

        BigDecimal totalCobrado = BigDecimal.ZERO;
        long nPagos = 0;
        for (Object[] p : consulta("SELECT DATE_FORMAT(p.fecha, '%Y-%m'), SUM(p.monto), COUNT(*) FROM pagos p "
                + "WHERE p.club_id = :club AND p.estado = 'Pagado' AND p.fecha BETWEEN :desde AND :hasta GROUP BY 1",
                club, r)) {
            BigDecimal monto = dec(p[1]);
            meses.computeIfAbsent((String) p[0], k -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO })[1] = monto;
            totalCobrado = totalCobrado.add(monto);
            nPagos += ((Number) p[2]).longValue();
        }

        List<Mes> serie = new ArrayList<>();
        meses.forEach((k, v) -> serie.add(new Mes(k, v[0], v[1])));

        List<Item> porConcepto = items(consulta("SELECT COALESCE(f.concepto, 'OTROS'), SUM(f.total), COUNT(*) FROM facturas f "
                + "WHERE f.club_id = :club AND f.anulada = 0 AND f.fecha_emision BETWEEN :desde AND :hasta "
                + "GROUP BY 1 ORDER BY 2 DESC", club, r));
        List<Item> porMetodo = items(consulta("SELECT mp.metodo, SUM(p.monto), COUNT(*) FROM pagos p "
                + "JOIN metodos_pago mp ON mp.id = p.metodo_pago_id "
                + "WHERE p.club_id = :club AND p.estado = 'Pagado' AND p.fecha BETWEEN :desde AND :hasta "
                + "GROUP BY mp.metodo ORDER BY 2 DESC", club, r));

        return new Financiero(r.desde(), r.hasta(), new Totales(totalFacturado, nFacturas, totalCobrado, nPagos),
                serie, porConcepto, porMetodo, cartera(club, hoy));
    }

    /** Cartera de HOY (no depende del rango): facturas vigentes con saldo, y su antiguedad si ya vencieron. */
    @SuppressWarnings("unchecked")
    private Cartera cartera(Integer club, LocalDate hoy) {
        List<Object[]> filas = em.createNativeQuery(
                "SELECT t.saldo, t.fecha_vencimiento FROM ("
                        + "SELECT f.total - COALESCE((SELECT SUM(p.monto) FROM pagos p WHERE p.factura_id = f.id "
                        + "AND p.estado = 'Pagado'), 0) AS saldo, f.fecha_vencimiento "
                        + "FROM facturas f WHERE f.club_id = :club AND f.anulada = 0) t WHERE t.saldo > 0")
                .setParameter("club", club).getResultList();

        long conSaldo = 0, enMora = 0;
        BigDecimal saldo = BigDecimal.ZERO, saldoMora = BigDecimal.ZERO;
        long[] n = new long[4];
        BigDecimal[] s = { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO };
        for (Object[] f : filas) {
            BigDecimal monto = dec(f[0]);
            conSaldo++;
            saldo = saldo.add(monto);
            LocalDate venc = fecha(f[1]);
            if (venc != null && venc.isBefore(hoy)) {
                enMora++;
                saldoMora = saldoMora.add(monto);
                long dias = java.time.temporal.ChronoUnit.DAYS.between(venc, hoy);
                int i = dias <= 30 ? 0 : dias <= 60 ? 1 : dias <= 90 ? 2 : 3;
                n[i]++;
                s[i] = s[i].add(monto);
            }
        }
        String[] et = { "1 a 30 días", "31 a 60 días", "61 a 90 días", "Más de 90 días" };
        List<Tramo> tramos = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tramos.add(new Tramo(et[i], n[i], s[i]));
        }
        return new Cartera(conSaldo, saldo, enMora, saldoMora, tramos);
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> consulta(String sql, Integer club, Rango r) {
        return em.createNativeQuery(sql).setParameter("club", club)
                .setParameter("desde", Date.valueOf(r.desde())).setParameter("hasta", Date.valueOf(r.hasta()))
                .getResultList();
    }

    private static List<Item> items(List<Object[]> filas) {
        List<Item> out = new ArrayList<>();
        for (Object[] f : filas) {
            out.add(new Item(String.valueOf(f[0]), dec(f[1]), ((Number) f[2]).longValue()));
        }
        return out;
    }

    private static BigDecimal dec(Object o) {
        if (o == null) {
            return BigDecimal.ZERO;
        }
        return o instanceof BigDecimal b ? b : new BigDecimal(o.toString());
    }

    private static LocalDate fecha(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof LocalDate d) {
            return d;
        }
        if (o instanceof Date d) {
            return d.toLocalDate();
        }
        return LocalDate.parse(o.toString().substring(0, 10));
    }
}
