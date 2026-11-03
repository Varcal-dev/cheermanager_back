package com.varcal.cheermanager.security;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.varcal.cheermanager.Models.Clubes.Club;
import com.varcal.cheermanager.Models.Clubes.VigenciaSuscripcion;
import com.varcal.cheermanager.Service.Clubes.SuscripcionService;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Clubes.ClubRepository;
import com.varcal.cheermanager.tenant.TenantContext;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autentica con el JWT y fija el CLUB de la peticion (TenantContext) desde el claim "clubId".
 * Rechaza (queda sin autenticar) si: token en lista negra/invalido, token sin clubId (anterior a la
 * Fase 1), club suspendido, o el usuario ya no existe/esta inactivo DENTRO de ese club.
 * El TenantContext SIEMPRE se limpia al terminar (los hilos del servidor se reutilizan).
 *
 * Suscripcion (V11): si la suscripcion del club esta vencida (o cancelada) el club queda en SOLO LECTURA:
 * las peticiones GET/HEAD/OPTIONS pasan, y las que modifican datos (POST/PUT/PATCH/DELETE) reciben 402
 * con un mensaje legible. Quedan exentas las de sesion y las notificaciones del portal.
 */
@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    /** Rutas que siguen funcionando con la suscripcion vencida (cerrar sesion, renovar sesion, cambiar clave, avisos). */
    private static final List<String> RUTAS_EXENTAS = List.of(
            "/api/logout", "/api/refresh-token", "/api/change-password", "/api/portal/notificaciones");

    static final String MENSAJE_SOLO_LECTURA = "La suscripción de tu club está vencida: puedes consultar la información, "
            + "pero no crear ni modificar datos. Contacta a CheerManager para renovarla.";

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserRepository userRepository;
    private final ClubRepository clubRepository;

    public JwtTokenFilter(JwtUtil jwtUtil, TokenBlacklistService tokenBlacklistService,
            UserRepository userRepository, ClubRepository clubRepository) {
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
        this.userRepository = userRepository;
        this.clubRepository = clubRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            boolean soloLectura = autenticar(request);
            if (soloLectura && esEscritura(request) && !esRutaExenta(request)) {
                response.setStatus(402);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"success\":false,\"status\":402,\"message\":\""
                        + MENSAJE_SOLO_LECTURA + "\"}");
                return;
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /** @return true si la peticion quedo autenticada y el club esta en modo solo lectura por su suscripcion. */
    private boolean autenticar(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authHeader.substring(7);
        if (tokenBlacklistService.isBlacklisted(token) || !jwtUtil.isTokenValid(token)) {
            return false;
        }

        Claims claims = jwtUtil.getClaims(token);
        Long id = jwtUtil.getUserId(token);
        Integer clubId = jwtUtil.getClubId(token);
        if (clubId == null) {
            return false; // token viejo
        }
        // Se lee ANTES de fijar el club: la tabla clubes es global (no lleva tenant).
        Optional<Club> club = clubRepository.findById(clubId);
        if (club.isEmpty() || !Boolean.TRUE.equals(club.get().getActivo())) {
            return false; // club inexistente o suspendido
        }
        boolean soloLectura = VigenciaSuscripcion.de(club.get(), SuscripcionService.hoy()).soloLectura();

        TenantContext.set(clubId);
        // Con el club ya fijado, la busqueda esta filtrada por club: un token con un clubId que no
        // corresponde al usuario simplemente no lo encuentra. (@SQLRestriction filtra inactivos.)
        if (!userRepository.existsById(id)) {
            TenantContext.clear();
            return false;
        }

        String email = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> permisos = (List<String>) claims.get("permisos");
        List<SimpleGrantedAuthority> authorities = permisos != null
                ? permisos.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
                : List.of();

        User userDetails = new User(email, "", authorities);
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, token, authorities);
        authToken.setDetails(Map.of(
                "userId", id,
                "clubId", clubId,
                "token", token,
                "requestDetails", new WebAuthenticationDetailsSource().buildDetails(request)));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        return soloLectura;
    }

    private static boolean esEscritura(HttpServletRequest request) {
        String m = request.getMethod();
        return "POST".equalsIgnoreCase(m) || "PUT".equalsIgnoreCase(m)
                || "PATCH".equalsIgnoreCase(m) || "DELETE".equalsIgnoreCase(m);
    }

    private static boolean esRutaExenta(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return RUTAS_EXENTAS.stream().anyMatch(uri::startsWith);
    }
}
