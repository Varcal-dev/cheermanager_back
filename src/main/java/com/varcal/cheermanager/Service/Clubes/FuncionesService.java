package com.varcal.cheermanager.Service.Clubes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Clubes.Club;

/**
 * Funciones (modulos) que trae el plan de cada club (V39).
 *
 * Orden de decision: club Plataforma o club sin plan -> todo; si no, funciones del plan y luego las excepciones del
 * club (clubes_caracteristicas), que pueden agregar o quitar. Las tablas son de plataforma, asi que se leen con SQL
 * directo (no pasan por el filtro de tenant). El resultado se guarda 60 s por club para no consultar en cada peticion.
 *
 * Que ruta pertenece a que funcion lo define REGLAS. Las reglas "soloEscritura" dejan consultar siempre (otras
 * pantallas, como matricula, leen convenios o recordatorios) y solo frenan crear, editar o enviar.
 */
@Service
public class FuncionesService {

    public record Regla(String prefijo, String funcion, boolean soloEscritura) {
    }

    /** Una funcion que el club perdio al bajar de plan: solo lectura hasta esa fecha; despues, bloqueada. */
    public record Transicion(LocalDate soloLecturaHasta) {
    }

    public enum Estado {
        PLENA, SOLO_LECTURA, BLOQUEADA
    }

    public record Habilitadas(boolean todas, Set<String> codigos, Map<String, Transicion> transiciones) {
        public Habilitadas(boolean todas, Set<String> codigos) {
            this(todas, codigos, Map.of());
        }

        public boolean incluye(String funcion) {
            return todas || codigos.contains(funcion);
        }

        public Estado estado(String funcion, LocalDate hoy) {
            return estadoDe(incluye(funcion), transiciones.get(funcion), hoy);
        }
    }

    /** Fila para la pantalla "Plan y mis datos": una funcion en ventana o ya bloqueada tras bajar de plan. */
    public record Perdida(String funcion, String nombre, String estado, LocalDate soloLecturaHasta) {
    }

    public static final List<Regla> REGLAS = List.of(
            new Regla("/api/evaluaciones-rutina", "RUBRICA_RUTINAS", false),
            new Regla("/api/rosters", "ROSTER", false),
            new Regla("/api/viajes", "VIAJES", false),
            new Regla("/api/ranking", "RANKING", false),
            new Regla("/api/entrenador-gestion", "GESTION_ENTRENADORES", false),
            new Regla("/api/productos", "TIENDA", false),
            new Regla("/api/producto-variantes", "TIENDA", false),
            new Regla("/api/categorias-producto", "TIENDA", false),
            new Regla("/api/movimientos-inventario", "TIENDA", false),
            new Regla("/api/ventas", "TIENDA", false),
            new Regla("/api/convenios", "CONVENIOS", true),
            new Regla("/api/recordatorios-mora", "RECORDATORIOS_MORA", true),
            new Regla("/api/auditoria", "AUDITORIA", false),
            new Regla("/api/conciliacion", "CONCILIACION_BANCARIA", false),
            new Regla("/api/sedes", "MULTISEDE", false));

    private static final long VIGENCIA_CACHE_MS = 60_000;

    private record Guardada(Habilitadas valor, long hasta) {
    }

    private final JdbcTemplate jdbc;
    private final Map<Integer, Guardada> cache = new ConcurrentHashMap<>();

    public FuncionesService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ------------------------------------------------------------------ puro (probado sin base de datos)

    /** Regla que aplica a esta peticion, o vacio si la ruta no tiene puerta. OPTIONS (preflight CORS) nunca se frena. */
    public static Optional<Regla> reglaDe(String uri, String metodo) {
        if (uri == null || "OPTIONS".equalsIgnoreCase(metodo)) {
            return Optional.empty();
        }
        boolean escritura = "POST".equalsIgnoreCase(metodo) || "PUT".equalsIgnoreCase(metodo)
                || "PATCH".equalsIgnoreCase(metodo) || "DELETE".equalsIgnoreCase(metodo);
        for (Regla r : REGLAS) {
            boolean coincide = uri.equals(r.prefijo()) || uri.startsWith(r.prefijo() + "/");
            if (coincide && (!r.soloEscritura() || escritura)) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    /** Incluida -> plena; perdida dentro de su ventana -> solo lectura; perdida y fuera de la ventana -> bloqueada. */
    public static Estado estadoDe(boolean incluida, Transicion t, LocalDate hoy) {
        if (incluida) {
            return Estado.PLENA;
        }
        return t != null && !hoy.isAfter(t.soloLecturaHasta()) ? Estado.SOLO_LECTURA : Estado.BLOQUEADA;
    }

    /** Funciones que estaban en el plan anterior y no estan en el nuevo. */
    public static Set<String> perdidas(Set<String> antes, Set<String> despues) {
        Set<String> r = new HashSet<>(antes);
        r.removeAll(despues);
        return r;
    }

    /** Funciones del plan, mas las excepciones del club (true agrega, false quita). */
    public static Set<String> resolver(Set<String> delPlan, Map<String, Boolean> excepciones) {
        Set<String> r = new HashSet<>(delPlan);
        excepciones.forEach((codigo, habilitada) -> {
            if (Boolean.TRUE.equals(habilitada)) {
                r.add(codigo);
            } else {
                r.remove(codigo);
            }
        });
        return r;
    }

    // ------------------------------------------------------------------ con base de datos

    public Habilitadas habilitadas(int clubId) {
        long ahora = System.currentTimeMillis();
        Guardada g = cache.get(clubId);
        if (g != null && g.hasta() > ahora) {
            return g.valor();
        }
        Habilitadas v = calcular(clubId);
        cache.put(clubId, new Guardada(v, ahora + VIGENCIA_CACHE_MS));
        return v;
    }

    /** Para usarlo tras cambiar el plan o las excepciones de un club sin esperar el minuto de cache. */
    public void olvidar(int clubId) {
        cache.remove(clubId);
    }

    private Habilitadas calcular(int clubId) {
        if (clubId == Club.PLATAFORMA_ID) {
            return new Habilitadas(true, Set.of());
        }
        List<Integer> planes = jdbc.queryForList("SELECT plan_id FROM clubes WHERE id = ?", Integer.class, clubId);
        Integer planId = planes.isEmpty() ? null : planes.get(0);
        if (planId == null) {
            return new Habilitadas(true, Set.of()); // club sin plan asignado: no se le quita nada
        }
        Set<String> delPlan = new HashSet<>(jdbc.queryForList(
                "SELECT caracteristica FROM planes_caracteristicas WHERE plan_id = ? AND habilitada = 1",
                String.class, planId));
        Map<String, Boolean> excepciones = new HashMap<>();
        for (Map<String, Object> fila : jdbc.queryForList(
                "SELECT caracteristica, habilitada FROM clubes_caracteristicas WHERE club_id = ?", clubId)) {
            Object h = fila.get("habilitada");
            boolean activa = h instanceof Boolean b ? b : h instanceof Number n && n.intValue() != 0;
            excepciones.put((String) fila.get("caracteristica"), activa);
        }
        Map<String, Transicion> transiciones = new HashMap<>();
        for (Map<String, Object> fila : jdbc.queryForList(
                "SELECT caracteristica, solo_lectura_hasta FROM clubes_caracteristicas_transicion WHERE club_id = ?",
                clubId)) {
            transiciones.put((String) fila.get("caracteristica"),
                    new Transicion(((java.sql.Date) fila.get("solo_lectura_hasta")).toLocalDate()));
        }
        return new Habilitadas(false, resolver(delPlan, excepciones), transiciones);
    }

    /**
     * Al cambiar de plan: lo que el club pierde entra en ventana de solo lectura (dias del plan de origen) y lo que
     * gana deja de estar en ventana. No borra datos. Si el club no tenia plan o el plan no cambio, no hace nada.
     */
    public void alCambiarPlan(int clubId, Integer planAnteriorId, Integer planNuevoId) {
        if (planAnteriorId == null || planNuevoId == null || planAnteriorId.equals(planNuevoId)) {
            return;
        }
        Set<String> antes = funcionesDelPlan(planAnteriorId);
        Set<String> despues = funcionesDelPlan(planNuevoId);
        Integer dias = jdbc.queryForObject("SELECT dias_solo_lectura_al_bajar FROM planes_saas WHERE id = ?",
                Integer.class, planAnteriorId);
        java.sql.Date hasta = java.sql.Date.valueOf(SuscripcionService.hoy().plusDays(dias == null ? 30 : dias));
        for (String f : perdidas(antes, despues)) {
            jdbc.update("INSERT INTO clubes_caracteristicas_transicion (club_id, caracteristica, plan_origen_id, "
                    + "solo_lectura_hasta) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE solo_lectura_hasta = "
                    + "VALUES(solo_lectura_hasta), plan_origen_id = VALUES(plan_origen_id)",
                    clubId, f, planAnteriorId, hasta);
        }
        // una funcion que el plan nuevo trae nunca debe quedar en ventana
        for (String f : despues) {
            jdbc.update("DELETE FROM clubes_caracteristicas_transicion WHERE club_id = ? AND caracteristica = ?", clubId, f);
        }
        olvidar(clubId);
    }

    /** Para la pantalla del club: funciones perdidas con su estado de hoy (las que el plan actual ya trae no salen). */
    public List<Perdida> perdidasDelClub(int clubId) {
        Habilitadas h = habilitadas(clubId);
        LocalDate hoy = SuscripcionService.hoy();
        List<Perdida> r = new ArrayList<>();
        for (Map<String, Object> fila : jdbc.queryForList(
                "SELECT t.caracteristica, c.nombre, t.solo_lectura_hasta FROM clubes_caracteristicas_transicion t "
                        + "JOIN caracteristicas_saas c ON c.codigo = t.caracteristica WHERE t.club_id = ? ORDER BY c.orden",
                clubId)) {
            String f = (String) fila.get("caracteristica");
            if (h.incluye(f)) {
                continue;
            }
            LocalDate hasta = ((java.sql.Date) fila.get("solo_lectura_hasta")).toLocalDate();
            r.add(new Perdida(f, (String) fila.get("nombre"), h.estado(f, hoy).name(), hasta));
        }
        return r;
    }

    private Set<String> funcionesDelPlan(int planId) {
        return new HashSet<>(jdbc.queryForList(
                "SELECT caracteristica FROM planes_caracteristicas WHERE plan_id = ? AND habilitada = 1",
                String.class, planId));
    }
}
