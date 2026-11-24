package com.varcal.cheermanager.security;

import java.net.InetAddress;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP real del cliente, en UN solo lugar (limite de intentos, registro de accesos, auditoria, aceptaciones legales...).
 *
 * <ul>
 *   <li>Si quien conecta es un proxy local o privado (Caddy/Nginx en el servidor, red Docker, red interna del hosting)
 *       se usa la ULTIMA entrada de X-Forwarded-For: es la que anade nuestro proxy con la IP que el vio. Las entradas
 *       anteriores las puede escribir el propio cliente.</li>
 *   <li>Si quien conecta es una IP publica, el encabezado se ignora (no hay proxy nuestro delante).</li>
 *   <li>El valor se VALIDA como IP literal (nunca se resuelve por DNS) y se normaliza: asi siempre cabe en las
 *       columnas VARCHAR(45) de log_accesos y auditoria, y un encabezado malformado no llega a la base de datos.</li>
 * </ul>
 * server.forward-headers-strategy=framework NO modifica getRemoteAddr(), por eso no se usa aqui.
 * Si hay un CDN u otro proxy delante del proxy propio, ese proxy debe reescribir X-Forwarded-For con la IP real del
 * cliente (en Caddy: trusted_proxies); de lo contrario todos aparecen con la IP del CDN.
 */
public final class ClienteIp {

    /** Valor cuando no hay forma de conocer la IP (no deberia ocurrir con un contenedor de servlets). */
    public static final String DESCONOCIDA = "desconocida";

    private static final Pattern IPV4 = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$");
    private static final Pattern IPV6_CARACTERES = Pattern.compile("^[0-9a-fA-F:.]{2,45}$");

    private ClienteIp() {
    }

    public static String de(HttpServletRequest request) {
        String remota = normalizar(request.getRemoteAddr());
        String xff = request.getHeader("X-Forwarded-For");
        if (remota != null && xff != null && !xff.isBlank() && esProxyConfiable(remota)) {
            String[] partes = xff.split(",");
            String candidata = normalizar(partes[partes.length - 1]);
            if (candidata != null) {
                return candidata;
            }
        }
        return remota != null ? remota : DESCONOCIDA;
    }

    /** Proxy "nuestro": loopback, redes privadas (10/8, 172.16/12, 192.168/16), enlace local, fc00::/7 y 100.64.0.0/10. */
    static boolean esProxyConfiable(String remota) {
        String ip = normalizar(remota);
        if (ip == null) return false;
        try {
            InetAddress a = InetAddress.getByName(ip); // IP literal ya validada: no hace consulta DNS
            byte[] b = a.getAddress();
            boolean ula = b.length == 16 && (b[0] & 0xFE) == 0xFC;                       // fc00::/7
            boolean compartida = b.length == 4 && (b[0] & 0xFF) == 100 && (b[1] & 0xC0) == 64; // 100.64.0.0/10 (hosting)
            return a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress() || ula || compartida;
        } catch (Exception e) {
            return false;
        }
    }

    /** IP literal valida (IPv4 o IPv6) en forma canonica, o null. Nunca consulta DNS. */
    static String normalizar(String texto) {
        if (texto == null) return null;
        String t = texto.trim();
        try {
            if (IPV4.matcher(t).matches()
                    || (t.indexOf(':') >= 0 && IPV6_CARACTERES.matcher(t).matches())) {
                // Con ':' Java solo lo interpreta como IPv6 literal; el IPv4 ya paso la expresion estricta.
                return InetAddress.getByName(t).getHostAddress();
            }
        } catch (Exception e) {
            // no es una IP valida
        }
        return null;
    }
}
