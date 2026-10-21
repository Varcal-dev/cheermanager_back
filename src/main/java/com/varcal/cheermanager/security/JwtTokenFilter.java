package com.varcal.cheermanager.security;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

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
 */
@Component
public class JwtTokenFilter extends OncePerRequestFilter {

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
            autenticar(request);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void autenticar(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        String token = authHeader.substring(7);
        if (tokenBlacklistService.isBlacklisted(token) || !jwtUtil.isTokenValid(token)) {
            return;
        }

        Claims claims = jwtUtil.getClaims(token);
        Long id = jwtUtil.getUserId(token);
        Integer clubId = jwtUtil.getClubId(token);
        if (clubId == null || !clubRepository.existsByIdAndActivoTrue(clubId)) {
            return; // token viejo o club suspendido
        }

        TenantContext.set(clubId);
        // Con el club ya fijado, la busqueda esta filtrada por club: un token con un clubId que no
        // corresponde al usuario simplemente no lo encuentra. (@SQLRestriction filtra inactivos.)
        if (!userRepository.existsById(id)) {
            TenantContext.clear();
            return;
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
    }
}