
package com.varcal.cheermanager.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Utils.RequiresPermission;

/**
 * Cada nombre usado en @RequiresPermission debe existir en los datos iniciales (migracion V2). Si no existe,
 * ese endpoint devuelve 403 a TODOS (incluido el administrador) y nadie lo nota hasta usarlo: asi se detecto
 * "ASIGNAR_PERMISO" (codigo) vs "asignar_permiso" (datos).
 */
class PermisosContraMigracionesTest {

    private static String leer(String recurso) throws Exception {
        try (InputStream in = PermisosContraMigracionesTest.class.getClassLoader().getResourceAsStream(recurso)) {
            assertTrue(in != null, "no se encuentra " + recurso);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Nombres declarados en `INSERT IGNORE INTO permisos (nombre) VALUES ... ;` de V2. */
    private static Set<String> permisosDeLosDatos() throws Exception {
        // V2 (catalogo inicial) + migraciones posteriores que agregan permisos
        // (V6: acceder_portal; V9: gestionar_recordatorios_mora).
        String sql = leer("db/migration/V2__datos_iniciales.sql") + "\n"
                + leer("db/migration/V6__portal_acudiente_deportista_y_solicitudes.sql") + "\n"
                + leer("db/migration/V9__recordatorios_mora.sql") + "\n"
                + leer("db/migration/V13__permisos_importar_exportar.sql") + "\n"
                + leer("db/migration/V15__caja_y_conciliacion.sql") + "\n"
                + leer("db/migration/V19__aceptacion_legal.sql") + "\n"
                + leer("db/migration/V20__permiso_exportar_datos_titular.sql") + "\n"
                + leer("db/migration/V21__anonimizacion_titulares.sql") + "\n"
                + leer("db/migration/V32__comunicados_y_calendario.sql") + "\n"
                + leer("db/migration/V34__lesiones_y_retorno.sql");
        // Sin comentarios SQL: V2 tiene un ";" dentro de un comentario ("...concreto; plural = ...") que cortaba el
        // bloque INSERT y dejaba ~28 permisos reales fuera del catalogo, haciendo fallar esta prueba.
        sql = sql.replaceAll("(?m)--[^\\n]*", "");
        Set<String> nombres = new HashSet<>();
        Matcher bloque = Pattern.compile("INSERT\\s+IGNORE\\s+INTO\\s+permisos\\s*\\(nombre\\)\\s*VALUES(.*?);",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(sql);
        while (bloque.find()) {
            Matcher n = Pattern.compile("\\('([^']+)'\\)").matcher(bloque.group(1));
            while (n.find()) nombres.add(n.group(1));
        }
        return nombres;
    }

    private static Set<String> permisosUsadosEnCodigo() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Set<String> usados = new TreeSet<>();
        for (BeanDefinition bd : scanner.findCandidateComponents("com.varcal.cheermanager")) {
            Class<?> c = Class.forName(bd.getBeanClassName());
            for (Method m : c.getDeclaredMethods()) {
                if (!AnnotatedElementUtils.hasAnnotation(m, RequestMapping.class)) continue;
                RequiresPermission rp = m.getAnnotation(RequiresPermission.class);
                if (rp != null) usados.add(rp.value());
            }
        }
        return usados;
    }

    @Test
    void todoPermisoUsadoEnCodigoExisteEnLosDatosIniciales() throws Exception {
        Set<String> enDatos = permisosDeLosDatos();
        assertFalse(enDatos.isEmpty(), "no se pudo leer la lista de permisos de V2");
        List<String> faltan = new ArrayList<>();
        for (String p : permisosUsadosEnCodigo()) {
            if (!enDatos.contains(p)) faltan.add(p);
        }
        assertTrue(faltan.isEmpty(), "Permisos usados en @RequiresPermission que NO existen en V2: " + faltan);
    }
}
