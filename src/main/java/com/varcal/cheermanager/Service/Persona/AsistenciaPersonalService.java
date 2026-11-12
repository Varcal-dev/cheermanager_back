package com.varcal.cheermanager.Service.Persona;

import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Personas.AsistenciaEntrenador;
import com.varcal.cheermanager.Service.Carnet.CarnetService;
import com.varcal.cheermanager.Service.Clubes.SuscripcionService;
import com.varcal.cheermanager.Service.Financiero.FacturaService;
import com.varcal.cheermanager.Service.Reportes.ReportesService;
import com.varcal.cheermanager.repository.Personas.AsistenciaEntrenadorRepository;
import com.varcal.cheermanager.repository.Personas.EntrenadorRepository;
import com.varcal.cheermanager.tenant.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Asistencia del personal (entrenadores) con el carnet QR (CM2). Una fila por entrenador y dia: la primera lectura
 * marca la entrada y la siguiente la salida. Las lecturas se fechan con la hora del servidor (zona del club), no la
 * del dispositivo; la correccion manual queda registrada como MANUAL.
 */
@Service
public class AsistenciaPersonalService {

    /** Una segunda lectura del mismo carnet dentro de este margen se toma como doble lectura, no como salida. */
    public static final int MARGEN_DOBLE_LECTURA_MIN = 2;

    public record Resultado(String evento, String nombre, LocalDate fecha, LocalTime horaEntrada,
            LocalTime horaSalida, String mensaje) {
    }

    public record Fila(Integer id, Integer entrenadorId, String nombre, LocalDate fecha, LocalTime horaEntrada,
            LocalTime horaSalida, Double horas, String origen) {
    }

    public record TotalEntrenador(Integer entrenadorId, String nombre, long dias, double horas) {
    }

    public record Reporte(LocalDate desde, LocalDate hasta, List<Fila> filas, List<TotalEntrenador> totales) {
    }

    public record Manual(Integer entrenadorId, LocalDate fecha, LocalTime horaEntrada, LocalTime horaSalida) {
    }

    @PersistenceContext
    private EntityManager em;
    @Autowired
    private CarnetService carnets;
    @Autowired
    private AsistenciaEntrenadorRepository repo;
    @Autowired
    private EntrenadorRepository entrenadores;

    public static LocalTime ahora() {
        return LocalTime.now(SuscripcionService.ZONA).truncatedTo(ChronoUnit.SECONDS);
    }

    @Transactional
    public Resultado escanear(String codigo) {
        CarnetService.CarnetPersonal c = carnets.verificarEntrenador(codigo);
        if (!c.vigente()) {
            throw new IllegalStateException(c.nombre() + ": carnet no vigente (estado: " + c.estado() + ").");
        }
        LocalDate hoy = SuscripcionService.hoy();
        LocalTime ahora = ahora();
        AsistenciaEntrenador a = repo.findByEntrenadorIdAndFecha(c.entrenadorId(), hoy).orElse(null);
        if (a == null) {
            a = nueva(c.entrenadorId(), hoy, ahora, null, "ESCANEO");
            repo.save(a);
            return new Resultado("ENTRADA", c.nombre(), hoy, ahora, null, "Entrada registrada.");
        }
        if (a.getHoraSalida() != null) {
            return new Resultado("COMPLETO", c.nombre(), hoy, a.getHoraEntrada(), a.getHoraSalida(),
                    "Ya tiene entrada y salida registradas hoy.");
        }
        if (Duration.between(a.getHoraEntrada(), ahora).toMinutes() < MARGEN_DOBLE_LECTURA_MIN) {
            return new Resultado("REPETIDO", c.nombre(), hoy, a.getHoraEntrada(), null,
                    "Entrada ya registrada hace un momento.");
        }
        a.setHoraSalida(ahora);
        repo.save(a);
        return new Resultado("SALIDA", c.nombre(), hoy, a.getHoraEntrada(), ahora, "Salida registrada.");
    }

    /** Alta o correccion manual de un dia (olvido de carnet, error de lectura). */
    @Transactional
    public Fila guardarManual(Manual m) {
        if (m.entrenadorId() == null || m.fecha() == null || m.horaEntrada() == null)
            throw new IllegalArgumentException("Indica entrenador, fecha y hora de entrada.");
        if (m.fecha().isAfter(SuscripcionService.hoy()))
            throw new IllegalArgumentException("No se puede registrar asistencia de una fecha futura.");
        if (m.horaSalida() != null && !m.horaSalida().isAfter(m.horaEntrada()))
            throw new IllegalArgumentException("La salida debe ser posterior a la entrada.");
        entrenadores.findById(m.entrenadorId())
                .orElseThrow(() -> new IllegalArgumentException("Entrenador no encontrado."));
        AsistenciaEntrenador a = repo.findByEntrenadorIdAndFecha(m.entrenadorId(), m.fecha())
                .orElseGet(() -> nueva(m.entrenadorId(), m.fecha(), m.horaEntrada(), null, "MANUAL"));
        a.setHoraEntrada(m.horaEntrada());
        a.setHoraSalida(m.horaSalida());
        a.setOrigen("MANUAL");
        a.setRegistradoPor(FacturaService.usuarioActualId());
        a = repo.save(a);
        return fila(a.getId());
    }

    @Transactional
    public void eliminar(Integer id) {
        AsistenciaEntrenador a = repo.findById(id).orElseThrow(() -> new RuntimeException("Registro no encontrado"));
        repo.delete(a);
    }

    @Transactional(readOnly = true)
    public Reporte reporte(LocalDate desde, LocalDate hasta, Integer entrenadorId) {
        ReportesService.Rango r = ReportesService.rango(desde, hasta, SuscripcionService.hoy());
        return new Reporte(r.desde(), r.hasta(), filas(r.desde(), r.hasta(), entrenadorId, null),
                totales(r.desde(), r.hasta(), entrenadorId));
    }

    @Transactional(readOnly = true)
    public List<Fila> deHoy() {
        LocalDate hoy = SuscripcionService.hoy();
        return filas(hoy, hoy, null, null);
    }

    private Fila fila(Integer id) {
        List<Fila> l = filas(null, null, null, id);
        return l.isEmpty() ? null : l.get(0);
    }

    private AsistenciaEntrenador nueva(Integer entrenadorId, LocalDate fecha, LocalTime entrada, LocalTime salida,
            String origen) {
        AsistenciaEntrenador a = new AsistenciaEntrenador();
        a.setEntrenadorId(entrenadorId);
        a.setFecha(fecha);
        a.setHoraEntrada(entrada);
        a.setHoraSalida(salida);
        a.setOrigen(origen);
        a.setRegistradoPor(FacturaService.usuarioActualId());
        return a;
    }

    private static final String BASE = "SELECT a.id, a.entrenador_id, CONCAT(per.nombre, ' ', per.apellidos), a.fecha, "
            + "a.hora_entrada, a.hora_salida, a.origen FROM asistencia_entrenador a "
            + "JOIN entrenadores e ON e.id = a.entrenador_id JOIN personas per ON per.id = e.persona_id "
            + "WHERE a.club_id = :club ";

    @SuppressWarnings("unchecked")
    private List<Fila> filas(LocalDate desde, LocalDate hasta, Integer entrenadorId, Integer id) {
        StringBuilder sql = new StringBuilder(BASE);
        if (id != null)
            sql.append("AND a.id = :id ");
        if (desde != null)
            sql.append("AND a.fecha BETWEEN :desde AND :hasta ");
        if (entrenadorId != null)
            sql.append("AND a.entrenador_id = :ent ");
        sql.append("ORDER BY a.fecha DESC, per.nombre, per.apellidos");
        var q = em.createNativeQuery(sql.toString()).setParameter("club", TenantContext.require());
        if (id != null)
            q.setParameter("id", id);
        if (desde != null)
            q.setParameter("desde", Date.valueOf(desde)).setParameter("hasta", Date.valueOf(hasta));
        if (entrenadorId != null)
            q.setParameter("ent", entrenadorId);
        List<Fila> out = new ArrayList<>();
        for (Object[] o : (List<Object[]>) q.getResultList()) {
            LocalTime in = hora(o[4]);
            LocalTime sal = hora(o[5]);
            Double h = sal == null ? null : Math.round(Duration.between(in, sal).toMinutes() / 60.0 * 100.0) / 100.0;
            out.add(new Fila(((Number) o[0]).intValue(), ((Number) o[1]).intValue(), (String) o[2],
                    o[3] instanceof Date d ? d.toLocalDate() : LocalDate.parse(o[3].toString()), in, sal, h,
                    (String) o[6]));
        }
        return out;
    }

    private static LocalTime hora(Object o) {
        if (o == null)
            return null;
        if (o instanceof java.sql.Time t)
            return t.toLocalTime();
        return LocalTime.parse(o.toString());
    }

    private List<TotalEntrenador> totales(LocalDate desde, LocalDate hasta, Integer entrenadorId) {
        Map<Integer, double[]> acc = new LinkedHashMap<>();
        Map<Integer, String> nombres = new LinkedHashMap<>();
        for (Fila f : filas(desde, hasta, entrenadorId, null)) {
            double[] v = acc.computeIfAbsent(f.entrenadorId(), k -> new double[2]);
            v[0]++;
            v[1] += f.horas() == null ? 0 : f.horas();
            nombres.putIfAbsent(f.entrenadorId(), f.nombre());
        }
        List<TotalEntrenador> out = new ArrayList<>();
        acc.forEach((k, v) -> out.add(new TotalEntrenador(k, nombres.get(k), (long) v[0],
                Math.round(v[1] * 100.0) / 100.0)));
        out.sort((a, b) -> a.nombre().compareToIgnoreCase(b.nombre()));
        return out;
    }
}
