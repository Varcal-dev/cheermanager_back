package com.varcal.cheermanager.Service.Importacion;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Lectura y escritura de CSV sin dependencias externas.
 *
 * Lectura: acepta UTF-8 (con o sin BOM) y, si el archivo no es UTF-8 valido, Windows-1252 (lo que guarda Excel en
 * Windows con "CSV (delimitado por comas)"). Detecta el separador (; , o tabulador), comillas dobles con "" escapado,
 * saltos de linea dentro de comillas y finales de linea CRLF / LF / CR. Ignora las lineas vacias.
 *
 * Escritura: UTF-8 con BOM (Excel lo abre con los acentos bien), separador ";" (el de Excel en espanol) y CRLF.
 * Protege contra inyeccion de formulas: una celda que empieza por = @ (o + - seguido de algo que no es numero)
 * se escribe con una comilla simple delante.
 */
public final class CsvUtil {

    public static final char SEPARADOR_ESCRITURA = ';';

    private CsvUtil() {
    }

    // ------------------------------------------------------------------ lectura

    public static List<String[]> leer(byte[] datos) {
        return leerTexto(decodificar(datos));
    }

    static String decodificar(byte[] datos) {
        String texto;
        try {
            texto = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(datos)).toString();
        } catch (CharacterCodingException e) {
            texto = new String(datos, Charset.forName("windows-1252"));
        }
        if (!texto.isEmpty() && texto.charAt(0) == '﻿') {
            texto = texto.substring(1);
        }
        return texto;
    }

    static char detectarSeparador(String texto) {
        int pc = 0, coma = 0, tab = 0;
        boolean comillas = false;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '"') {
                comillas = !comillas;
            } else if (!comillas) {
                if (c == '\n' || c == '\r') {
                    break; // basta con la primera linea (el encabezado)
                }
                if (c == ';') pc++;
                else if (c == ',') coma++;
                else if (c == '\t') tab++;
            }
        }
        if (pc >= coma && pc >= tab) return ';';
        return coma >= tab ? ',' : '\t';
    }

    static List<String[]> leerTexto(String texto) {
        char sep = detectarSeparador(texto);
        List<String[]> filas = new ArrayList<>();
        List<String> fila = new ArrayList<>();
        StringBuilder celda = new StringBuilder();
        boolean comillas = false;
        int n = texto.length();
        for (int i = 0; i < n; i++) {
            char c = texto.charAt(i);
            if (comillas) {
                if (c == '"') {
                    if (i + 1 < n && texto.charAt(i + 1) == '"') {
                        celda.append('"');
                        i++;
                    } else {
                        comillas = false;
                    }
                } else {
                    celda.append(c);
                }
            } else if (c == '"' && celda.toString().isBlank()) {
                comillas = true;
                celda.setLength(0);
            } else if (c == sep) {
                fila.add(celda.toString().trim());
                celda.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < n && texto.charAt(i + 1) == '\n') {
                    i++;
                }
                cerrarFila(filas, fila, celda);
            } else {
                celda.append(c);
            }
        }
        if (celda.length() > 0 || !fila.isEmpty()) {
            cerrarFila(filas, fila, celda);
        }
        return filas;
    }

    private static void cerrarFila(List<String[]> filas, List<String> fila, StringBuilder celda) {
        fila.add(celda.toString().trim());
        celda.setLength(0);
        boolean vacia = fila.stream().allMatch(String::isEmpty);
        if (!vacia) {
            filas.add(fila.toArray(new String[0]));
        }
        fila.clear();
    }

    // ------------------------------------------------------------------ escritura

    public static byte[] escribir(List<List<String>> filas) {
        StringBuilder sb = new StringBuilder("﻿");
        for (List<String> fila : filas) {
            for (int i = 0; i < fila.size(); i++) {
                if (i > 0) sb.append(SEPARADOR_ESCRITURA);
                sb.append(celda(fila.get(i)));
            }
            sb.append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    static String celda(String valor) {
        if (valor == null || valor.isEmpty()) {
            return "";
        }
        String v = protegerFormula(valor);
        boolean comillas = v.indexOf(SEPARADOR_ESCRITURA) >= 0 || v.indexOf('"') >= 0
                || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0;
        if (!comillas) {
            return v;
        }
        return '"' + v.replace("\"", "\"\"") + '"';
    }

    static String protegerFormula(String v) {
        char c = v.charAt(0);
        boolean peligroso = c == '=' || c == '@' || c == '\t' || c == '\r';
        if (!peligroso && (c == '+' || c == '-') && v.length() > 1) {
            char d = v.charAt(1);
            peligroso = !(Character.isDigit(d) || d == ' ' || d == '(');
        }
        return peligroso ? "'" + v : v;
    }
}
