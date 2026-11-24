package com.varcal.cheermanager.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.LongSupplier;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limite de intentos por IP en los endpoints publicos de credenciales (SEC-01). Complementa el bloqueo por cuenta
 * de AuthService: frena a quien prueba claves contra muchas cuentas desde una IP, y el correo masivo por
 * "olvide mi contrasena".
 *
 * <ul>
 *   <li>/api/login: cuenta solo los intentos FALLIDOS (4xx), para no castigar a un gimnasio o colegio donde muchas
 *       personas entran a la vez con exito desde la misma IP.</li>
 *   <li>/api/forgot-password: cuenta toda solicitud (siempre responde 200 para no revelar correos).</li>
 *   <li>/api/reset-password: cuenta solo los fallidos (token invalido).</li>
 * </ul>
 *
 * No es un @Component a proposito: se registra solo en la cadena de Spring Security (SecurityConfig) y no tambien
 * como filtro del contenedor.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final boolean activo;
    private final LimitadorIntentos login;
    private final LimitadorIntentos forgot;
    private final LimitadorIntentos reset;

    public RateLimitFilter(boolean activo,
                           int loginMax, long loginVentanaMs,
                           int forgotMax, long forgotVentanaMs,
                           int resetMax, long resetVentanaMs,
                           LongSupplier reloj) {
        this.activo = activo;
        this.login = new LimitadorIntentos(loginMax, loginVentanaMs, reloj);
        this.forgot = new LimitadorIntentos(forgotMax, forgotVentanaMs, reloj);
        this.reset = new LimitadorIntentos(resetMax, resetVentanaMs, reloj);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LimitadorIntentos limitador = activo && "POST".equalsIgnoreCase(request.getMethod())
                ? limitadorPara(rutaRelativa(request)) : null;
        if (limitador == null) {
            chain.doFilter(request, response);
            return;
        }

        String ip = ClienteIp.de(request);
        long espera = limitador.segundosParaReintentar(ip);
        if (espera > 0) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(espera));
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"success\":false,\"message\":"
                    + "\"Demasiados intentos. Espera unos minutos antes de volver a intentar.\"}");
            return;
        }

        boolean cuentaTodo = limitador == forgot;
        if (cuentaTodo) limitador.registrar(ip);
        chain.doFilter(request, response);
        int status = response.getStatus();
        if (!cuentaTodo && status >= 400 && status < 500) limitador.registrar(ip);
    }

    private LimitadorIntentos limitadorPara(String ruta) {
        return switch (ruta) {
            case "/api/login" -> login;
            case "/api/forgot-password" -> forgot;
            case "/api/reset-password" -> reset;
            default -> null;
        };
    }

    private static String rutaRelativa(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        return ctx != null && !ctx.isEmpty() && uri.startsWith(ctx) ? uri.substring(ctx.length()) : uri;
    }
}
