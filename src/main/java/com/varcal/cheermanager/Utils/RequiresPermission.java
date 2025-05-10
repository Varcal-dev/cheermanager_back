package com.varcal.cheermanager.Utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // Se aplica a métodos
@Retention(RetentionPolicy.RUNTIME) // Disponible en tiempo de ejecución
public @interface RequiresPermission {
    String value(); // Nombre del permiso requerido
}