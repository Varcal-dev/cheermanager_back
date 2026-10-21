package com.varcal.cheermanager.Utils;

import java.security.SecureRandom;

/** Genera claves temporales aleatorias (sin caracteres ambiguos como 0/O o 1/l/I). */
public final class ClaveTemporal {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private ClaveTemporal() {
    }

    public static String generar() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }
}