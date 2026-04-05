package com.varcal.cheermanager.Utils;

import java.text.Normalizer;

public class UsernameUtils {

    public static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "") // elimina tildes
                .replaceAll("\\s+", "")          // elimina espacios
                .toLowerCase();
    }

    public static String primerToken(String texto) {
        if (texto == null) return "";
        return texto.trim().split("\\s+")[0];
    }
}
