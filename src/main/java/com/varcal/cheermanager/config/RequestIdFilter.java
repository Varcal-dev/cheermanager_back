package com.varcal.cheermanager.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Asigna un id a cada peticion (MDC "rid") y lo devuelve en la cabecera X-Request-Id.
 * Si el cliente o el proxy ya envian uno valido se reutiliza; asi se puede seguir una
 * peticion desde el navegador hasta el log. No registra cuerpos, cabeceras ni tokens.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    private static final Pattern VALIDO = Pattern.compile("^[A-Za-z0-9._-]{8,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String rid = request.getHeader(HEADER);
        if (rid == null || !VALIDO.matcher(rid).matches()) {
            rid = UUID.randomUUID().toString().substring(0, 12);
        }
        MDC.put("rid", rid);
        response.setHeader(HEADER, rid);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("rid");
        }
    }
}