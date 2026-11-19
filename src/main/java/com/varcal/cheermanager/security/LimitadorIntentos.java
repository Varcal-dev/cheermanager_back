package com.varcal.cheermanager.security;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Ventana deslizante por clave (IP) en memoria: como mucho {@code max} eventos cada {@code ventanaMs}.
 * Sin dependencias de servlet para poder probarla en aislamiento. Vive en memoria de UNA instancia: se reinicia con
 * la API y no se comparte entre instancias (suficiente con una sola instancia; ver SEC-01).
 */
public class LimitadorIntentos {

    private static final int MAX_CLAVES = 5000;

    private final int max;
    private final long ventanaMs;
    private final LongSupplier reloj;
    private final Map<String, Deque<Long>> eventos = new ConcurrentHashMap<>();

    public LimitadorIntentos(int max, long ventanaMs, LongSupplier reloj) {
        if (max < 1 || ventanaMs < 1) throw new IllegalArgumentException("max y ventanaMs deben ser positivos");
        this.max = max;
        this.ventanaMs = ventanaMs;
        this.reloj = reloj;
    }

    /** Segundos que faltan para que la clave pueda volver a intentar; 0 si hoy no esta bloqueada. */
    public long segundosParaReintentar(String clave) {
        Deque<Long> cola = eventos.get(clave);
        if (cola == null) return 0;
        long ahora = reloj.getAsLong();
        synchronized (cola) {
            purgar(cola, ahora);
            if (cola.size() < max) return 0;
            long libre = cola.peekFirst() + ventanaMs - ahora;
            return Math.max(1, (libre + 999) / 1000);
        }
    }

    /** Anota un evento (intento) para la clave. */
    public void registrar(String clave) {
        long ahora = reloj.getAsLong();
        Deque<Long> cola = eventos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (cola) {
            purgar(cola, ahora);
            cola.addLast(ahora);
        }
        if (eventos.size() > MAX_CLAVES) {
            eventos.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    purgar(e.getValue(), ahora);
                    return e.getValue().isEmpty();
                }
            });
        }
    }

    private void purgar(Deque<Long> cola, long ahora) {
        while (!cola.isEmpty() && ahora - cola.peekFirst() >= ventanaMs) cola.pollFirst();
    }
}
