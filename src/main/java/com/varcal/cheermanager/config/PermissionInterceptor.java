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
import com.varcal.cheermanager.Utils.SinPermisoEspecifico;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autorizacion por permiso, FAIL-CLOSED.
 *
 * Regla: todo handler de /api/** debe tener @RequiresPermission("x") o
 * @SinPermisoEspecifico (en el metodo o en la clase). Si no tiene ninguna, se
 * deniega (403). Antes, un endpoint sin anotacion quedaba abierto a cualquier
 * usuario autenticado.
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Autowired
    private AuthService authService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true; // recursos estaticos, preflight CORS, etc.
        }

        // Endpoints que solo exigen sesion (o publicos por SecurityConfig).
        if (handlerMethod.hasMethodAnnotation(SinPermisoEspecifico.class)
                || handlerMethod.getBeanType().isAnnotationPresent(SinPermisoEspecifico.class)) {
            return true;
        }

        RequiresPermission requiresPermission = handlerMethod.getMethodAnnotation(RequiresPermission.class);
        if (requiresPermission == null) {
            // Fail-closed: nadie decidio quien puede entrar aqui.
            return deny(response, HttpServletResponse.SC_FORBIDDEN, "Acceso denegado");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return deny(response, HttpServletResponse.SC_UNAUTHORIZED, "Usuario no autenticado");
        }

        Integer userId = extraerUserId(authentication);
        if (userId == null) {
            return deny(response, HttpServletResponse.SC_UNAUTHORIZED, "Usuario no autenticado");
        }

        // Se valida en vivo contra la BD (no contra el claim del JWT) para que un
        // cambio de permisos aplique de inmediato.
        if (!authService.tienePermiso(userId, requiresPermission.value())) {
            return deny(response, HttpServletResponse.SC_FORBIDDEN,
                    "No tienes permiso para acceder a esta funcionalidad");
        }
        return true;
    }

    private boolean deny(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"status\":" + status + ",\"message\":\"" + message + "\"}");
        return false;
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