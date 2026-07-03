package com.varcal.cheermanager.Utils;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// JwtTokenFilter ya guarda el userId del token en Authentication.getDetails()
// como un Map (junto con "token" y "requestDetails") — esta clase es el
// único punto donde se lee eso, para que ningún Service tenga que conocer
// la forma interna de ese Map. Disponible en cualquier Service porque
// SecurityContextHolder usa un ThreadLocal que vive durante todo el request,
// no solo en el Controller.
public final class AuthenticatedUserUtil {

    private AuthenticatedUserUtil() {
    }

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No hay un usuario autenticado en el contexto actual");
        }

        Object details = authentication.getDetails();
        if (!(details instanceof Map)) {
            throw new RuntimeException("No se pudo resolver el usuario autenticado (detalles de sesión inválidos)");
        }

        Object userId = ((Map<?, ?>) details).get("userId");
        if (userId == null) {
            throw new RuntimeException("El token no contiene userId");
        }

        // Se devuelve Long porque así está tipeado hoy UserRepository
        // (JpaRepository<Usuario, Long>), aunque Usuario.id sea Integer —
        // es una inconsistencia preexistente del proyecto, ver nota en
        // UserRepository. Cuando se unifique, ajustar acá también.
        return ((Number) userId).longValue();
    }
}