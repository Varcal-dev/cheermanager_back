package com.varcal.cheermanager.config;

import com.varcal.cheermanager.Utils.RequiresPermission;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        // Verificar si el handler es un método
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;

            // Verificar si el método tiene la anotación @RequiresPermission
            RequiresPermission requiresPermission = handlerMethod.getMethodAnnotation(RequiresPermission.class);
            if (requiresPermission != null) {
                String permisoRequerido = requiresPermission.value();

                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !authentication.isAuthenticated()) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Usuario no autenticado");
                    return false;
                }

                boolean hasPermission = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(authority -> authority.equals(permisoRequerido));

                if (!hasPermission) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write("No tienes permiso para acceder a esta funcionalidad");
                    return false;
                }
            }
        }

        // Continuar con la ejecución si no hay restricciones
        return true;
    }
}