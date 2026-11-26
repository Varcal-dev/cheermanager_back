package com.varcal.cheermanager.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Cache en memoria con expiracion (TTL), tamano maximo e invalidacion, sin dependencias (PERF-02).
 *
 * <ul>
 *   <li>Un valor {@code null} del cargador NO se guarda (p. ej. "el usuario no existe": un usuario recien creado debe
 *       funcionar al instante).</li>
 *   <li>Cada invalidacion sube una "generacion". Si una carga empezo ANTES de una invalidacion, su resultado se
 *       devuelve a quien lo pidio pero NO se guarda: evita que un dato leido antes de un cambio quede en el cache
 *       despues de invalidarlo.</li>
 *   <li>Llegado el maximo se purgan las entradas vencidas; si aun no hay sitio, simplemente no se guarda (el cache
 *       nunca crece sin limite).</li>
 *   <li>Vive en la memoria de UNA instancia: otra instancia de la API no se entera de las invalidaciones y se
 *       actualiza solo al vencer el TTL.</li>
 * </ul>
 */
public final class CacheTtl<K, V> {

    private record Entrada<V>(V valor, long expira) {
    }

    private final long ttlMs;
    private final int maximo;
    private final LongSupplier reloj;
    private final Map<K, Entrada<V>> mapa = new ConcurrentHashMap<>();
    private final Object cerrojo = new Object(); // protege "comprobar generacion + guardar" frente a invalidar
    private long generacion; // solo se toca bajo cerrojo

    public CacheTtl(long ttlMs, int maximo, LongSupplier reloj) {
        if (ttlMs < 1 || maximo < 1) throw new IllegalArgumentException("ttlMs y maximo deben ser positivos");
        this.ttlMs = ttlMs;
        this.maximo = maximo;
        this.reloj = reloj;
    }

    /** Devuelve el valor guardado y vigente o lo carga. Si el cargador devuelve null, devuelve null y no guarda. */
    public V obtener(K clave, Supplier<V> cargador) {
        long ahora = reloj.getAsLong();
        Entrada<V> e = mapa.get(clave);
        if (e != null && e.expira() > ahora) {
            return e.valor();
        }
        long generacionAlEmpezar = leerGeneracion();
        V valor = cargador.get();
        if (valor != null) {
            synchronized (cerrojo) {
                if (generacion == generacionAlEmpezar) {
                    guardar(clave, valor, ahora + ttlMs);
                }
            }
        }
        return valor;
    }

    public void invalidar(K clave) {
        synchronized (cerrojo) {
            generacion++;
            mapa.remove(clave);
        }
    }

    public void invalidarSi(Predicate<K> criterio) {
        synchronized (cerrojo) {
            generacion++;
            mapa.keySet().removeIf(criterio);
        }
    }

    public void invalidarTodo() {
        synchronized (cerrojo) {
            generacion++;
            mapa.clear();
        }
    }

    /** Entradas guardadas (incluye vencidas aun no purgadas). Para pruebas y diagnostico. */
    public int tamano() {
        return mapa.size();
    }

    private long leerGeneracion() {
        synchronized (cerrojo) {
            return generacion;
        }
    }

    // Se llama bajo cerrojo.
    private void guardar(K clave, V valor, long expira) {
        if (mapa.size() >= maximo && !mapa.containsKey(clave)) {
            long ahora = reloj.getAsLong();
            mapa.values().removeIf(en -> en.expira() <= ahora);
            if (mapa.size() >= maximo) {
                return; // sin sitio: no se guarda
            }
        }
        mapa.put(clave, new Entrada<>(valor, expira));
    }
}
