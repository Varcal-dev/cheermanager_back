package com.varcal.cheermanager.Service.Clubes;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Clubes.Club;
import com.varcal.cheermanager.tenant.TenantLookupRepository;

/**
 * Paquetes de deportistas adicionales (V40). Los asigna el superadmin; suman cupo al limite del plan del club.
 * Tablas de plataforma: se leen con SQL directo, fuera del filtro de tenant.
 */
@Service
public class PaquetesService {

    public record Paquete(Integer id, String nombre, int deportistasExtra, BigDecimal precioMensual) {
    }

    public record Asignacion(Integer id, Integer paqueteId, String nombre, int cantidad, int deportistasExtra,
            LocalDate fechaInicio, LocalDate fechaFin, BigDecimal precioAcordado, boolean vigente) {
    }

    public record Vista(Integer planId, int maximoPaquetes, int paquetesVigentes, int cupoExtra,
            List<Paquete> ofrecidos, List<Asignacion> asignados) {
    }

    public record AsignarRequest(Integer paqueteId, Integer cantidad, LocalDate fechaInicio, LocalDate fechaFin,
            BigDecimal precioAcordado) {
    }

    private final JdbcTemplate jdbc;
    private final TenantLookupRepository lookup;

    public PaquetesService(JdbcTemplate jdbc, TenantLookupRepository lookup) {
        this.jdbc = jdbc;
        this.lookup = lookup;
    }

    // ------------------------------------------------------------------ puro (probado sin base de datos)

    public static boolean vigente(LocalDate inicio, LocalDate fin, LocalDate hoy) {
        return inicio != null && !inicio.isAfter(hoy) && (fin == null || !fin.isBefore(hoy));
    }

    /** Un club no puede tener mas paquetes vigentes que el tope de su plan. */
    public static void validarTope(int vigentes, int cantidad, int tope) {
        if (cantidad < 1) {
            throw new RuntimeException("La cantidad de paquetes debe ser al menos 1");
        }
        if (vigentes + cantidad > tope) {
            throw new RuntimeException("Tu plan permite hasta " + tope + " paquete(s) y el club ya tiene " + vigentes
                    + " vigente(s). Para más cupo, sube de plan.");
        }
    }

    /** Al quitar un paquete, los deportistas activos deben seguir cabiendo (igual que al bajar de plan, L10). */
    public static void validarQueCaben(Integer limiteBase, int extraDespues, long activos) {
        if (limiteBase == null) {
            return;
        }
        int limite = limiteBase + extraDespues;
        if (activos > limite) {
            throw new RuntimeException("No se puede quitar el paquete: el club tiene " + activos
                    + " deportistas activos y el cupo quedaría en " + limite + ". Retira deportistas primero.");
        }
    }

    // ------------------------------------------------------------------ consulta

    /** Deportistas extra que suman los paquetes vigentes del club en el plan indicado. */
    public int cupoExtra(int clubId, int planId) {
        Integer n = jdbc.queryForObject(
                "SELECT COALESCE(SUM(cp.cantidad * p.deportistas_extra), 0) FROM clubes_paquetes_deportistas cp "
                        + "JOIN paquetes_deportistas_saas p ON p.id = cp.paquete_id "
                        + "WHERE cp.club_id = ? AND p.plan_id = ? AND cp.fecha_inicio <= ? "
                        + "AND (cp.fecha_fin IS NULL OR cp.fecha_fin >= ?)",
                Integer.class, clubId, planId, Date.valueOf(SuscripcionService.hoy()), Date.valueOf(SuscripcionService.hoy()));
        return n == null ? 0 : n;
    }

    public Vista vista(int clubId) {
        Integer planId = planDe(clubId);
        if (planId == null) {
            return new Vista(null, 0, 0, 0, List.of(), List.of());
        }
        LocalDate hoy = SuscripcionService.hoy();
        List<Paquete> ofrecidos = jdbc.queryForList(
                "SELECT id, nombre, deportistas_extra, precio_mensual FROM paquetes_deportistas_saas "
                        + "WHERE plan_id = ? AND activo = 1 ORDER BY deportistas_extra", planId).stream()
                .map(f -> new Paquete(((Number) f.get("id")).intValue(), (String) f.get("nombre"),
                        ((Number) f.get("deportistas_extra")).intValue(), (BigDecimal) f.get("precio_mensual")))
                .toList();
        List<Asignacion> asignados = jdbc.queryForList(
                "SELECT cp.id, cp.paquete_id, p.nombre, cp.cantidad, p.deportistas_extra, cp.fecha_inicio, cp.fecha_fin, "
                        + "cp.precio_acordado FROM clubes_paquetes_deportistas cp "
                        + "JOIN paquetes_deportistas_saas p ON p.id = cp.paquete_id "
                        + "WHERE cp.club_id = ? AND p.plan_id = ? ORDER BY cp.fecha_inicio DESC, cp.id DESC",
                clubId, planId).stream().map(f -> {
                    LocalDate ini = ((Date) f.get("fecha_inicio")).toLocalDate();
                    LocalDate fin = f.get("fecha_fin") == null ? null : ((Date) f.get("fecha_fin")).toLocalDate();
                    return new Asignacion(((Number) f.get("id")).intValue(), ((Number) f.get("paquete_id")).intValue(),
                            (String) f.get("nombre"), ((Number) f.get("cantidad")).intValue(),
                            ((Number) f.get("deportistas_extra")).intValue(), ini, fin,
                            (BigDecimal) f.get("precio_acordado"), vigente(ini, fin, hoy));
                }).toList();
        int vigentes = asignados.stream().filter(Asignacion::vigente).mapToInt(Asignacion::cantidad).sum();
        int extra = asignados.stream().filter(Asignacion::vigente).mapToInt(a -> a.cantidad() * a.deportistasExtra()).sum();
        return new Vista(planId, topeDe(planId), vigentes, extra, ofrecidos, asignados);
    }

    // ------------------------------------------------------------------ administracion (superadmin)

    @Transactional
    public Vista asignar(int clubId, AsignarRequest req, String quien) {
        if (clubId == Club.PLATAFORMA_ID) {
            throw new RuntimeException("El club de la plataforma no usa paquetes");
        }
        Integer planId = planDe(clubId);
        if (planId == null) {
            throw new RuntimeException("El club no tiene un plan asignado");
        }
        if (req.paqueteId() == null) {
            throw new RuntimeException("Elige el paquete");
        }
        List<Map<String, Object>> p = jdbc.queryForList(
                "SELECT plan_id, activo FROM paquetes_deportistas_saas WHERE id = ?", req.paqueteId());
        if (p.isEmpty() || !Boolean.TRUE.equals(bool(p.get(0).get("activo")))) {
            throw new RuntimeException("El paquete no existe o no está activo");
        }
        if (((Number) p.get(0).get("plan_id")).intValue() != planId) {
            throw new RuntimeException("Ese paquete no corresponde al plan del club");
        }
        LocalDate hoy = SuscripcionService.hoy();
        LocalDate inicio = req.fechaInicio() != null ? req.fechaInicio() : hoy;
        if (req.fechaFin() != null && req.fechaFin().isBefore(inicio)) {
            throw new RuntimeException("La fecha fin no puede ser anterior al inicio");
        }
        if (req.precioAcordado() != null && req.precioAcordado().signum() < 0) {
            throw new RuntimeException("El precio acordado no puede ser negativo");
        }
        int cantidad = req.cantidad() == null ? 1 : req.cantidad();
        int vigentes = vista(clubId).paquetesVigentes();
        validarTope(vigentes, cantidad, topeDe(planId));
        jdbc.update("INSERT INTO clubes_paquetes_deportistas (club_id, paquete_id, cantidad, fecha_inicio, fecha_fin, "
                + "precio_acordado, registrado_por) VALUES (?, ?, ?, ?, ?, ?, ?)",
                clubId, req.paqueteId(), cantidad, Date.valueOf(inicio),
                req.fechaFin() == null ? null : Date.valueOf(req.fechaFin()), req.precioAcordado(),
                quien == null ? null : quien.substring(0, Math.min(quien.length(), 80)));
        return vista(clubId);
    }

    /** Termina un paquete hoy (o lo borra si aun no habia empezado). */
    @Transactional
    public Vista terminar(int clubId, int asignacionId) {
        Integer planId = planDe(clubId);
        if (planId == null) {
            throw new RuntimeException("El club no tiene un plan asignado");
        }
        Vista antes = vista(clubId);
        Asignacion a = antes.asignados().stream().filter(x -> x.id() == asignacionId).findFirst()
                .orElseThrow(() -> new RuntimeException("La asignación no existe"));
        if (a.vigente()) {
            Integer base = jdbc.queryForObject("SELECT COALESCE(c.limite_deportistas, p.max_deportistas) FROM clubes c "
                    + "LEFT JOIN planes_saas p ON p.id = c.plan_id WHERE c.id = ?", Integer.class, clubId);
            validarQueCaben(base, antes.cupoExtra() - a.cantidad() * a.deportistasExtra(),
                    lookup.contarDeportistasActivos(clubId));
        }
        LocalDate hoy = SuscripcionService.hoy();
        if (a.fechaInicio().isAfter(hoy.minusDays(1))) {
            jdbc.update("DELETE FROM clubes_paquetes_deportistas WHERE id = ? AND club_id = ?", asignacionId, clubId);
        } else {
            jdbc.update("UPDATE clubes_paquetes_deportistas SET fecha_fin = ? WHERE id = ? AND club_id = ?",
                    Date.valueOf(hoy.minusDays(1)), asignacionId, clubId);
        }
        return vista(clubId);
    }

    // ------------------------------------------------------------------ utilidades

    private Integer planDe(int clubId) {
        List<Integer> r = jdbc.queryForList("SELECT plan_id FROM clubes WHERE id = ?", Integer.class, clubId);
        if (r.isEmpty()) {
            throw new RuntimeException("Club no encontrado");
        }
        return r.get(0);
    }

    private int topeDe(int planId) {
        Integer t = jdbc.queryForObject("SELECT max_paquetes_deportistas FROM planes_saas WHERE id = ?", Integer.class, planId);
        return t == null ? 0 : t;
    }

    private static Boolean bool(Object o) {
        return o instanceof Boolean b ? b : o instanceof Number n && n.intValue() != 0;
    }
}
