package com.varcal.cheermanager.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.varcal.cheermanager.Service.Auth.AuthService;
import com.varcal.cheermanager.Utils.RequiresPermission;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Autowired
    private AuthService authService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;

            RequiresPermission requiresPermission = handlerMethod.getMethodAnnotation(RequiresPermission.class);
            if (requiresPermission != null) {
                String permisoRequerido = requiresPermission.value();

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !authentication.isAuthenticated()) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Usuario no autenticado");
                    return false;
                }

                Integer userId = extraerUserId(authentication);
                if (userId == null) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Usuario no autenticado");
                    return false;
                }

                // Antes: comparaba contra el claim "permisos" del JWT, que queda
                // congelado desde el momento del login. Ahora valida en vivo contra
                // la DB (mismo mecanismo que usa GET /api/usuario/permisos), así que
                // un permiso recién asignado a un rol aplica de inmediato, sin
                // necesidad de que el usuario vuelva a loguearse.
                boolean hasPermission = authService.tienePermiso(userId, permisoRequerido);

                if (!hasPermission) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("No tienes permiso para acceder a esta funcionalidad");
                    return false;
                }
            }
        }

        return true;
    }

    private Integer extraerUserId(Authentication authentication) {
        Object details = authentication.getDetails();
        if (details instanceof Map<?, ?> detailMap) {
            Object userId = detailMap.get("userId");
            if (userId instanceof Number) {
                return ((Number) userId).intValue();
            }
        }
        return null;
    }
}