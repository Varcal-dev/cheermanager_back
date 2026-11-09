package com.varcal.cheermanager.Service.Notificaciones;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Arma archivos de calendario iCalendar (.ics, RFC 5545) para Google Calendar, Apple y Outlook. C7. */
public final class IcsBuilder {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    static final String ZONA = "America/Bogota";

    private IcsBuilder() {
    }

    public static String calendario(String nombre, List<String> eventos) {
        StringBuilder sb = new StringBuilder();
        linea(sb, "BEGIN:VCALENDAR");
        linea(sb, "VERSION:2.0");
        linea(sb, "PRODID:-//CheerManager//Calendario//ES");
        linea(sb, "CALSCALE:GREGORIAN");
        linea(sb, "METHOD:PUBLISH");
        linea(sb, "X-WR-CALNAME:" + escapar(nombre));
        linea(sb, "X-WR-TIMEZONE:" + ZONA);
        // Colombia no tiene horario de verano: una sola regla basta.
        linea(sb, "BEGIN:VTIMEZONE");
        linea(sb, "TZID:" + ZONA);
        linea(sb, "BEGIN:STANDARD");
        linea(sb, "DTSTART:19700101T000000");
        linea(sb, "TZOFFSETFROM:-0500");
        linea(sb, "TZOFFSETTO:-0500");
        linea(sb, "TZNAME:COT");
        linea(sb, "END:STANDARD");
        linea(sb, "END:VTIMEZONE");
        for (String e : eventos) {
            sb.append(e);
        }
        linea(sb, "END:VCALENDAR");
        return sb.toString();
    }

    /** Clase semanal: primera fecha, hora de inicio y fin, repite cada semana el mismo dia. */
    public static String eventoSemanal(String uid, LocalDateTime estampa, LocalDate primeraFecha, LocalTime inicio,
            LocalTime fin, String diaIcs, String titulo, String descripcion) {
        StringBuilder sb = new StringBuilder();
        linea(sb, "BEGIN:VEVENT");
        linea(sb, "UID:" + uid);
        linea(sb, "DTSTAMP:" + estampa.atOffset(ZoneOffset.UTC).format(FECHA_HORA) + "Z");
        linea(sb, "DTSTART;TZID=" + ZONA + ":" + primeraFecha.atTime(inicio).format(FECHA_HORA));
        linea(sb, "DTEND;TZID=" + ZONA + ":" + primeraFecha.atTime(fin).format(FECHA_HORA));
        linea(sb, "RRULE:FREQ=WEEKLY;BYDAY=" + diaIcs);
        linea(sb, "SUMMARY:" + escapar(titulo));
        if (descripcion != null && !descripcion.isBlank()) {
            linea(sb, "DESCRIPTION:" + escapar(descripcion));
        }
        linea(sb, "END:VEVENT");
        return sb.toString();
    }

    /** Evento de dia completo (competencias, eventos del club). */
    public static String eventoDiaCompleto(String uid, LocalDateTime estampa, LocalDate fecha, String titulo,
            String lugar, String descripcion) {
        StringBuilder sb = new StringBuilder();
        linea(sb, "BEGIN:VEVENT");
        linea(sb, "UID:" + uid);
        linea(sb, "DTSTAMP:" + estampa.atOffset(ZoneOffset.UTC).format(FECHA_HORA) + "Z");
        linea(sb, "DTSTART;VALUE=DATE:" + fecha.format(FECHA));
        linea(sb, "DTEND;VALUE=DATE:" + fecha.plusDays(1).format(FECHA)); // el fin de un dia completo es exclusivo
        linea(sb, "SUMMARY:" + escapar(titulo));
        if (lugar != null && !lugar.isBlank()) {
            linea(sb, "LOCATION:" + escapar(lugar));
        }
        if (descripcion != null && !descripcion.isBlank()) {
            linea(sb, "DESCRIPTION:" + escapar(descripcion));
        }
        linea(sb, "END:VEVENT");
        return sb.toString();
    }

    static String escapar(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\r\n", "\\n")
                .replace("\n", "\\n").replace("\r", "\\n");
    }

    /** RFC 5545: lineas de maximo 75 octetos; la continuacion empieza con un espacio. Se corta por caracter, no por byte. */
    static void linea(StringBuilder sb, String texto) {
        int limite = 75;
        int usados = 0;
        for (int i = 0; i < texto.length();) {
            int cp = texto.codePointAt(i);
            int bytes = new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8).length;
            if (usados + bytes > limite) {
                sb.append("\r\n ");
                limite = 74; // el espacio inicial cuenta como un octeto
                usados = 0;
            }
            sb.appendCodePoint(cp);
            usados += bytes;
            i += Character.charCount(cp);
        }
        sb.append("\r\n");
    }
}
