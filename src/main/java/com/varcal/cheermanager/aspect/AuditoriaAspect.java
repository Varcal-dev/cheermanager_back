package com.varcal.cheermanager.aspect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.varcal.cheermanager.Models.Funcionalidad.Auditoria;
import com.varcal.cheermanager.Service.Funcionalidad.AuditoriaService;
import com.varcal.cheermanager.repository.Funcionalidad.AuditoriaRepository;
import com.varcal.cheermanager.repository.Funcionalidad.LogAccesoRepository;

import jakarta.persistence.Table;

// ---------------------------------------------------------------------------
// Auditoria.java (Model) y AuditoriaRepository existían, pero NADA en todo
// el backend escribía en la tabla `auditoria` — ver el comentario que había
// quedado en DashboardAdministracionService.obtenerAuditoriaReciente(). Este
// aspecto es lo que finalmente la llena, sin tener que tocar cada
// Controller/Service uno por uno: intercepta las llamadas save()/delete()
// de TODOS los repositorios de com.varcal.cheermanager.repository y
// construye el registro de auditoría automáticamente.
//
// Requiere la dependencia spring-boot-starter-aop en el pom.xml (ya
// agregada). Spring Boot activa el auto-proxying de AOP solo con tenerla en
// el classpath, no hace falta @EnableAspectJAutoProxy adicional.
//
// GARANTÍA DE SEGURIDAD: cualquier fallo dentro de este aspecto (una
// entidad sin getId(), un problema de serialización, lo que sea) jamás debe
// impedir que la operación real de negocio se ejecute o se propague como
// error al usuario. Por eso cada bloque de auditoría está aislado en su
// propio try/catch alrededor de pjp.proceed().
// ---------------------------------------------------------------------------
@Aspect
@Component
public class AuditoriaAspect {

    @Autowired
    private AuditoriaService auditoriaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Nunca auditar la propia tabla de auditoría (evita un bucle infinito:
    // registrar() internamente llama a auditoriaRepository.save(), que
    // volvería a pasar por este mismo aspecto) ni los logs de acceso, que
    // ya tienen su propio mecanismo de registro en LogAccesoService.
    private boolean debeExcluirse(Object repositorio) {
        return repositorio instanceof AuditoriaRepository || repositorio instanceof LogAccesoRepository;
    }

    // Campos que jamás deben terminar en un registro de auditoría, sin
    // importar en qué entidad aparezcan (hashes de contraseña, tokens de
    // reset, etc.). Se compara en minúsculas y por "contains" para cubrir
    // variantes de nombre sin tener que enumerar cada entidad.
    private static final Set<String> FRAGMENTOS_CAMPO_EXCLUIDO =
            Set.of("password", "token", "secret");

    @Around(value = "execution(* com.varcal.cheermanager.repository..*.save(..)) "
            + "&& target(repositorio) && args(entidad)", argNames = "pjp,repositorio,entidad")
    public Object auditarGuardado(ProceedingJoinPoint pjp, Object repositorio, Object entidad) throws Throwable {
        if (debeExcluirse(repositorio)) {
            return pjp.proceed();
        }

        Object idAntes = obtenerId(entidad);
        boolean esNuevo = (idAntes == null);

        // En UPDATE, capturamos el estado en BD *antes* de que este save lo
        // sobreescriba — es la única forma de tener un "antes" real y no
        // solo el "después" dos veces.
        String valoresAnteriores = esNuevo ? null : obtenerSnapshotPrevio(repositorio, idAntes);

        Object resultado = pjp.proceed();

        try {
            Object entidadFinal = resultado != null ? resultado : entidad;
            String tabla = nombreTabla(entidadFinal.getClass());
            auditoriaService.registrar(
                    tabla,
                    esNuevo ? Auditoria.AccionEnum.INSERT : Auditoria.AccionEnum.UPDATE,
                    obtenerId(entidadFinal),
                    valoresAnteriores,
                    serializarSnapshot(entidadFinal));
        } catch (Exception ignored) {
            // best-effort — jamás debe afectar la respuesta real del save()
        }

        return resultado;
    }

    @Around(value = "execution(* com.varcal.cheermanager.repository..*.deleteById(..)) "
            + "&& target(repositorio) && args(id)", argNames = "pjp,repositorio,id")
    public Object auditarEliminacionPorId(ProceedingJoinPoint pjp, Object repositorio, Object id) throws Throwable {
        if (debeExcluirse(repositorio)) {
            return pjp.proceed();
        }

        Object entidadPrevia = obtenerEntidadPorId(repositorio, id);
        Object resultado = pjp.proceed();

        try {
            if (entidadPrevia != null) {
                auditoriaService.registrar(
                        nombreTabla(entidadPrevia.getClass()),
                        Auditoria.AccionEnum.DELETE,
                        id,
                        serializarSnapshot(entidadPrevia),
                        null);
            }
        } catch (Exception ignored) {
        }

        return resultado;
    }

    @Around(value = "execution(* com.varcal.cheermanager.repository..*.delete(..)) "
            + "&& target(repositorio) && args(entidad)", argNames = "pjp,repositorio,entidad")
    public Object auditarEliminacion(ProceedingJoinPoint pjp, Object repositorio, Object entidad) throws Throwable {
        if (debeExcluirse(repositorio)) {
            return pjp.proceed();
        }

        Object resultado = pjp.proceed();

        try {
            auditoriaService.registrar(
                    nombreTabla(entidad.getClass()),
                    Auditoria.AccionEnum.DELETE,
                    obtenerId(entidad),
                    serializarSnapshot(entidad),
                    null);
        } catch (Exception ignored) {
        }

        return resultado;
    }

    // --- Helpers de reflexión, todos tolerantes a fallos ------------------

    private Object obtenerId(Object entidad) {
        if (entidad == null) return null;
        try {
            Method m = entidad.getClass().getMethod("getId");
            return m.invoke(entidad);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Object obtenerEntidadPorId(Object repositorio, Object id) {
        try {
            Method m = repositorio.getClass().getMethod("findById", Object.class);
            Object resultado = m.invoke(repositorio, id);
            if (resultado instanceof Optional<?> opt && opt.isPresent()) {
                return opt.get();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String obtenerSnapshotPrevio(Object repositorio, Object id) {
        Object entidadPrevia = obtenerEntidadPorId(repositorio, id);
        return entidadPrevia != null ? serializarSnapshot(entidadPrevia) : null;
    }

    private String nombreTabla(Class<?> claseEntidad) {
        Table tabla = claseEntidad.getAnnotation(Table.class);
        if (tabla != null && tabla.name() != null && !tabla.name().isBlank()) {
            return tabla.name();
        }
        return claseEntidad.getSimpleName();
    }

    // Snapshot deliberadamente superficial: solo campos escalares propios
    // (String, número, boolean, enum, fecha), nunca relaciones (@ManyToOne,
    // @OneToMany, colecciones) — así evitamos por diseño tanto
    // LazyInitializationException fuera de transacción como recursión
    // infinita por relaciones bidireccionales. El costo es que el "antes/
    // después" no muestra cambios en relaciones (ej. reasignar un grupo no
    // se ve aquí), solo en los campos propios de la entidad.
    private String serializarSnapshot(Object entidad) {
        if (entidad == null) return null;
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            for (Field campo : obtenerTodosLosCampos(entidad.getClass())) {
                String nombreMinuscula = campo.getName().toLowerCase();
                boolean excluido = FRAGMENTOS_CAMPO_EXCLUIDO.stream().anyMatch(nombreMinuscula::contains);
                if (excluido || !esTipoSeguro(campo.getType())) {
                    continue;
                }
                campo.setAccessible(true);
                Object valor = campo.get(entidad);
                snapshot.put(campo.getName(), valor);
            }
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean esTipoSeguro(Class<?> tipo) {
        return tipo == String.class
                || Number.class.isAssignableFrom(tipo)
                || tipo == int.class || tipo == long.class || tipo == double.class || tipo == float.class
                || tipo == boolean.class || tipo == Boolean.class
                || tipo.isEnum()
                || tipo == LocalDate.class || tipo == LocalDateTime.class || tipo == LocalTime.class;
    }

    private List<Field> obtenerTodosLosCampos(Class<?> clase) {
        List<Field> campos = new ArrayList<>();
        Class<?> actual = clase;
        while (actual != null && actual != Object.class) {
            campos.addAll(List.of(actual.getDeclaredFields()));
            actual = actual.getSuperclass();
        }
        return campos;
    }
}