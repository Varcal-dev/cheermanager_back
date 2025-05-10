package com.varcal.cheermanager.config;

import com.varcal.cheermanager.Service.Auth.AuthService;
import com.varcal.cheermanager.Utils.RequiresPermission;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Autowired
    private AuthService authService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        // Verificar si el handler es un método
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;

            // Verificar si el método tiene la anotación @RequiresPermission
            RequiresPermission requiresPermission = handlerMethod.getMethodAnnotation(RequiresPermission.class);
            if (requiresPermission != null) {
                // Obtener el permiso requerido
                String permisoRequerido = requiresPermission.value();

                // Obtener el usuario de la sesión
                HttpSession session = request.getSession();
                Integer userId = (Integer) session.getAttribute("userId");

                if (userId == null) {
                    // Si no hay usuario en la sesión, devolver un error 401
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Usuario no autenticado");
                    return false;
                }

                if (!authService.tienePermiso(userId, permisoRequerido)) {
                    // Si no tiene permiso, devolver un error 403
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