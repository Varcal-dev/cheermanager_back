package com.varcal.cheermanager.Service.Reportes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Service.Reportes.ReportesService.Rango;
import com.varcal.cheermanager.tenant.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Analisis historico mensual del club: altas, asistencia, retencion (quienes entrenaron el mes anterior y siguen
 * entrenando), evaluaciones y cobrado, mas los deportistas con baja asistencia. Solo lectura, consultas nativas
 * filtradas a mano por club_id. Asiste = estados "Presente" y "Tarde".
 */
@Service
public class AnalisisService {

    /** Por debajo de este % de asistencia (con minimo de sesiones) se marca al deportista en riesgo. */
    public static final int UMBRAL_RIESGO = 60;
    public static final int MIN_SESIONES_RIESGO = 4;

    public record MesAnalisis(String mes, long altas, long sesiones, long asistidas, Double tasaAsistencia,
            long deportistasActivos, Double retencion, Double promedioEvaluacion, long evaluaciones,
            BigDecimal cobrado) {
    }

    public record Riesgo(Integer deportistaId, String nombre, long sesiones, long asistidas, double tasa) {
    }

    public record Resumen(long altas, Double tasaAsistencia, Double retencionPromedio, Double promedioEvaluacion,
            BigDecimal cobrado) {
    }

    public record Analisis(LocalDate desde, LocalDate hasta, Resumen resumen, List<MesAnalisis> meses,
            List<Riesgo> enRiesgo) {
    }

    @PersistenceContext
    private EntityManager em;

    @SuppressWarnings("unchecked")
    private List<Object[]> q(String sql, Integer club, Rango r) {
        return em.createNativeQuery(sql).setParameter("club", club).setParameter("desde", Date.valueOf(r.desde()))
                .setParameter("hasta", Date.valueOf(r.hasta())).getResultList();
    }

    private static long n(Object o) {
        return o == null ? 0 : ((Number) o).longValue();
    }

    private static Double pct(long a, long b) {
        return b == 0 ? null : Math.round(a * 1000.0 / b) / 10.0;
    }

    @Transactional(readOnly = true)
    public Analisis analizar(LocalDate desde, LocalDate hasta, LocalDate hoy) {
        Rango r = ReportesService.rango(desde, hasta, hoy);
        Integer club = TenantContext.require();

        Map<String, long[]> m = new TreeMap<>(); // altas, sesiones, asistidas, evalN, activos
        Map<String, double[]> ev = new HashMap<>(); // suma puntajes
        Map<String, BigDecimal> cob = new HashMap<>();
        for (YearMonth y = YearMonth.from(r.desde()); !y.isAfter(YearMonth.from(r.hasta())); y = y.plusMonths(1))
            m.put(y.toString(), new long[5]);

        for (Object[] o : q("SELECT DATE_FORMAT(d.fecha_inscripcion, '%Y-%m'), COUNT(*) FROM deportistas d "
                + "WHERE d.club_id = :club AND d.fecha_inscripcion BETWEEN :desde AND :hasta GROUP BY 1", club, r))
            m.computeIfAbsent((String) o[0], k -> new long[5])[0] = n(o[1]);

        for (Object[] o : q("SELECT DATE_FORMAT(a.fecha, '%Y-%m'), COUNT(*), "
                + "SUM(CASE WHEN ea.estado IN ('Presente','Tarde') THEN 1 ELSE 0 END) FROM asistencias a "
                + "JOIN estados_asistencia ea ON ea.id = a.estado_asistencia_id "
                + "WHERE a.club_id = :club AND a.fecha BETWEEN :desde AND :hasta GROUP BY 1", club, r)) {
            long[] v = m.computeIfAbsent((String) o[0], k -> new long[5]);
            v[1] = n(o[1]);
            v[2] = n(o[2]);
        }

        // Deportistas distintos que asistieron cada mes (para activos y retencion).
        Map<String, Set<Object>> asistentes = new TreeMap<>();
        for (Object[] o : q("SELECT DISTINCT DATE_FORMAT(a.fecha, '%Y-%m'), a.deportista_id FROM asistencias a "
                + "JOIN estados_asistencia ea ON ea.id = a.estado_asistencia_id "
                + "WHERE a.club_id = :club AND ea.estado IN ('Presente','Tarde') AND a.fecha BETWEEN :desde AND :hasta",
                club, r))
            asistentes.computeIfAbsent((String) o[0], k -> new HashSet<>()).add(o[1]);

        for (Object[] o : q("SELECT DATE_FORMAT(e.fecha, '%Y-%m'), COUNT(*), SUM(e.puntaje_obtenido) FROM evaluaciones e "
                + "WHERE e.club_id = :club AND e.fecha BETWEEN :desde AND :hasta GROUP BY 1", club, r)) {
            m.computeIfAbsent((String) o[0], k -> new long[5])[3] = n(o[1]);
            ev.put((String) o[0], new double[] { o[2] == null ? 0 : ((Number) o[2]).doubleValue() });
        }

        for (Object[] o : q("SELECT DATE_FORMAT(p.fecha, '%Y-%m'), SUM(p.monto) FROM pagos p "
                + "WHERE p.club_id = :club AND p.estado = 'Pagado' AND p.fecha BETWEEN :desde AND :hasta GROUP BY 1",
                club, r))
            cob.put((String) o[0], o[1] == null ? BigDecimal.ZERO : new BigDecimal(o[1].toString()));

        List<MesAnalisis> meses = new ArrayList<>();
        String previo = null;
        long altas = 0, ses = 0, asi = 0, evN = 0, retN = 0;
        double evSuma = 0, retSuma = 0;
        BigDecimal totalCob = BigDecimal.ZERO;
        for (var en : m.entrySet()) {
            String k = en.getKey();
            long[] v = en.getValue();
            Set<Object> act = asistentes.getOrDefault(k, Set.of());
            Double ret = null;
            if (previo != null && YearMonth.parse(previo).plusMonths(1).toString().equals(k)) {
                Set<Object> ant = asistentes.getOrDefault(previo, Set.of());
                if (!ant.isEmpty()) {
                    long siguen = ant.stream().filter(act::contains).count();
                    ret = pct(siguen, ant.size());
                    retSuma += ret;
                    retN++;
                }
            }
            Double prom = v[3] == 0 ? null
                    : BigDecimal.valueOf(ev.get(k)[0] / v[3]).setScale(2, RoundingMode.HALF_UP).doubleValue();
            BigDecimal c = cob.getOrDefault(k, BigDecimal.ZERO);
            meses.add(new MesAnalisis(k, v[0], v[1], v[2], pct(v[2], v[1]), act.size(), ret, prom, v[3], c));
            altas += v[0];
            ses += v[1];
            asi += v[2];
            evN += v[3];
            evSuma += ev.containsKey(k) ? ev.get(k)[0] : 0;
            totalCob = totalCob.add(c);
            previo = k;
        }

        // Deportistas con baja asistencia en el rango.
        List<Riesgo> riesgo = new ArrayList<>();
        for (Object[] o : q("SELECT d.id, CONCAT(per.nombre, ' ', per.apellidos), COUNT(*), "
                + "SUM(CASE WHEN ea.estado IN ('Presente','Tarde') THEN 1 ELSE 0 END) FROM asistencias a "
                + "JOIN estados_asistencia ea ON ea.id = a.estado_asistencia_id "
                + "JOIN deportistas d ON d.id = a.deportista_id JOIN personas per ON per.id = d.persona_id "
                + "WHERE a.club_id = :club AND a.fecha BETWEEN :desde AND :hasta GROUP BY d.id, per.nombre, per.apellidos "
                + "HAVING COUNT(*) >= " + MIN_SESIONES_RIESGO
                + " AND SUM(CASE WHEN ea.estado IN ('Presente','Tarde') THEN 1 ELSE 0 END) * 100 < " + UMBRAL_RIESGO
                + " * COUNT(*) ORDER BY SUM(CASE WHEN ea.estado IN ('Presente','Tarde') THEN 1 ELSE 0 END) / COUNT(*) LIMIT 25",
                club, r)) {
            long t = n(o[2]), a = n(o[3]);
            riesgo.add(new Riesgo(((Number) o[0]).intValue(), (String) o[1], t, a, pct(a, t)));
        }

        Resumen res = new Resumen(altas, pct(asi, ses), retN == 0 ? null : Math.round(retSuma / retN * 10) / 10.0,
                evN == 0 ? null : Math.round(evSuma / evN * 100) / 100.0, totalCob);
        return new Analisis(r.desde(), r.hasta(), res, meses, riesgo);
    }
}
