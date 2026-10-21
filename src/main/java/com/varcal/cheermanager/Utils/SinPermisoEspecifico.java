package com.varcal.cheermanager.Utils;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca de forma EXPLICITA un endpoint que solo exige estar autenticado (o que es
 * publico, si SecurityConfig lo permite), sin un permiso granular.
 *
 * PermissionInterceptor es "fail-closed": todo endpoint de /api/** debe llevar
 * {@link RequiresPermission} o esta anotacion. Si no lleva ninguna, responde 403.
 * Asi un controller nuevo olvidado nunca queda abierto por accidente.
 *
 * Usar solo para: catalogos de lectura, perfil propio (/me) y endpoints de auth.
 */
@Documented
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface SinPermisoEspecifico {
    /** Motivo por el que no requiere permiso (documentacion para el revisor). */
    String value() default "";
}