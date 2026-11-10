package com.varcal.cheermanager.Service.Clubes;

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

    public record Habilitadas(boolean todas, Set<String> codigos) {
        public boolean incluye(String funcion) {
            return todas || codigos.contains(funcion);
        }
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
        return new Habilitadas(false, resolver(delPlan, excepciones));
    }
}
