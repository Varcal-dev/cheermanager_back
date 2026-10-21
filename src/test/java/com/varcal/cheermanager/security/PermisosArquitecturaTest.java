package com.varcal.cheermanager.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Utils.RequiresPermission;
import com.varcal.cheermanager.Utils.SinPermisoEspecifico;

/**
 * Guardia de seguridad: TODO endpoint REST debe declarar quien puede usarlo.
 * Si agregas un controller/metodo sin @RequiresPermission o @SinPermisoEspecifico, este test
 * falla (y en runtime PermissionInterceptor lo denegaria con 403).
 */
class PermisosArquitecturaTest {

    @Test
    void todoEndpointDeclaraSuPermiso() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<String> sinProteger = new ArrayList<>();
        for (BeanDefinition bd : scanner.findCandidateComponents("com.varcal.cheermanager")) {
            Class<?> c = Class.forName(bd.getBeanClassName());
            boolean claseMarcada = c.isAnnotationPresent(SinPermisoEspecifico.class);
            for (Method m : c.getDeclaredMethods()) {
                boolean esEndpoint = AnnotatedElementUtils.hasAnnotation(m, RequestMapping.class);
                if (!esEndpoint) continue;
                boolean ok = claseMarcada
                        || m.isAnnotationPresent(RequiresPermission.class)
                        || m.isAnnotationPresent(SinPermisoEspecifico.class);
                if (!ok) sinProteger.add(c.getSimpleName() + "#" + m.getName());
            }
        }
        assertTrue(sinProteger.isEmpty(), "Endpoints sin permiso declarado: " + sinProteger);
    }
}