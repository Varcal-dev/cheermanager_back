package com.varcal.cheermanager.aspect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.varcal.cheermanager.Models.Funcionalidad.Auditoria;
import com.varcal.cheermanager.Service.Funcionalidad.AuditoriaService;
import com.varcal.cheermanager.repository.Funcionalidad.AuditoriaRepository;
import com.varcal.cheermanager.repository.Funcionalidad.LogAccesoRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    @PersistenceContext
    private EntityManager em;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Mas de este numero de filas en un saveAll/deleteAll: se registra UNA fila-resumen en vez de una por fila. */
    private static final int MAX_FILAS_DETALLADAS = 25;

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
        String valoresAnteriores = esNuevo ? null : obtenerSnapshotPrevio(repositorio, entidad, idAntes);

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

    // saveAll y deleteAll(Iterable) tambien modifican datos; antes pasaban sin auditarse.
    @Around(value = "execution(* com.varcal.cheermanager.repository..*.saveAll(..)) "
            + "&& target(repositorio) && args(entidades)", argNames = "pjp,repositorio,entidades")
    public Object auditarGuardadoMasivo(ProceedingJoinPoint pjp, Object repositorio, Iterable<?> entidades)
            throws Throwable {
        if (debeExcluirse(repositorio)) {
            return pjp.proceed();
        }

        List<Object> lista = new ArrayList<>();
        List<String> previos = new ArrayList<>();
        List<Boolean> nuevos = new ArrayList<>();
        try {
            for (Object e : entidades) {
                Object id = obtenerId(e);
                lista.add(e);
                nuevos.add(id == null);
                previos.add(id == null ? null : obtenerSnapshotPrevio(repositorio, e, id));
            }
        } catch (Exception ignored) {
            // best-effort
        }

        Object resultado = pjp.proceed();

        try {
            List<Object> guardadas = new ArrayList<>();
            if (resultado instanceof Iterable<?> it) {
                it.forEach(guardadas::add);
            }
            if (guardadas.size() != lista.size()) {
                guardadas = lista; // no se pueden emparejar con sus "antes": se auditan tal cual
                previos = new ArrayList<>(java.util.Collections.nCopies(lista.size(), (String) null));
                nuevos = new ArrayList<>(java.util.Collections.nCopies(lista.size(), Boolean.TRUE));
            }
            if (guardadas.size() > MAX_FILAS_DETALLADAS) {
                registrarResumen(guardadas, Auditoria.AccionEnum.INSERT);
            } else {
                for (int i = 0; i < guardadas.size(); i++) {
                    Object g = guardadas.get(i);
                    auditoriaService.registrar(
                            nombreTabla(g.getClass()),
                            nuevos.get(i) ? Auditoria.AccionEnum.INSERT : Auditoria.AccionEnum.UPDATE,
                            obtenerId(g),
                            previos.get(i),
                            serializarSnapshot(g));
                }
            }
        } catch (Exception ignored) {
        }

        return resultado;
    }

    @Around(value = "execution(* com.varcal.cheermanager.repository..*.deleteAll(..)) "
            + "&& target(repositorio) && args(entidades)", argNames = "pjp,repositorio,entidades")
    public Object auditarEliminacionMasiva(ProceedingJoinPoint pjp, Object repositorio, Iterable<?> entidades)
            throws Throwable {
        if (debeExcluirse(repositorio)) {
            return pjp.proceed();
        }

        // Se fotografian ANTES de borrar: despues ya no se pueden leer.
        List<Object> lista = new ArrayList<>();
        List<String> fotos = new ArrayList<>();
        try {
            for (Object e : entidades) {
                lista.add(e);
                fotos.add(lista.size() <= MAX_FILAS_DETALLADAS ? serializarSnapshot(e) : null);
            }
        } catch (Exception ignored) {
        }

        Object resultado = pjp.proceed();

        try {
            if (lista.size() > MAX_FILAS_DETALLADAS) {
                registrarResumen(lista, Auditoria.AccionEnum.DELETE);
            } else {
                for (int i = 0; i < lista.size(); i++) {
                    Object e = lista.get(i);
                    auditoriaService.registrar(nombreTabla(e.getClass()), Auditoria.AccionEnum.DELETE,
                            obtenerId(e), fotos.get(i), null);
                }
            }
        } catch (Exception ignored) {
        }

        return resultado;
    }

    /** Una sola fila para un lote grande (p. ej. un comunicado enviado a 300 familias). */
    private void registrarResumen(List<Object> filas, Auditoria.AccionEnum accion) {
        if (filas.isEmpty()) {
            return;
        }
        String resumen = "{\"resumen\":\"" + filas.size() + " registros en una sola operacion\"}";
        auditoriaService.registrar(nombreTabla(filas.get(0).getClass()), accion, null,
                accion == Auditoria.AccionEnum.DELETE ? resumen : null,
                accion == Auditoria.AccionEnum.DELETE ? null : resumen);
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

    /**
     * Estado de la entidad ANTES de este cambio.
     *
     * Antes se leia con findById, pero la mayoria de los servicios hacen entidad.setX(...) y luego save(entidad)
     * dentro de la misma sesion: findById devolvia esa MISMA instancia ya modificada y el "antes" salia igual al
     * "despues". Ahora se toma el estado con el que Hibernate cargo la entidad (loadedState). Si la entidad no esta
     * gestionada (llega separada, p. ej. desde un DTO), no hay mutacion previa y findById si lee la fila original.
     * Si ni una ni otra sirven, devuelve null: la pantalla muestra "sin detalle del antes" en vez de un dato falso.
     */
    private String obtenerSnapshotPrevio(Object repositorio, Object entidad, Object id) {
        String cargado = snapshotDesdeEstadoCargado(entidad);
        if (cargado != null) {
            return cargado;
        }
        Object entidadPrevia = obtenerEntidadPorId(repositorio, id);
        if (entidadPrevia == null || entidadPrevia == entidad) {
            return null; // misma instancia ya modificada: el valor original no se puede recuperar por aqui
        }
        return serializarSnapshot(entidadPrevia);
    }

    private String snapshotDesdeEstadoCargado(Object entidad) {
        try {
            SessionImplementor sesion = em.unwrap(SessionImplementor.class);
            EntityEntry entrada = sesion.getPersistenceContextInternal().getEntry(entidad);
            if (entrada == null || entrada.getLoadedState() == null) {
                return null;
            }
            String[] nombres = entrada.getPersister().getPropertyNames();
            Object[] valores = entrada.getLoadedState();
            Map<String, Object> snapshot = new LinkedHashMap<>();
            if (entrada.getId() != null) {
                snapshot.put("id", entrada.getId());
            }
            for (int i = 0; i < nombres.length && i < valores.length; i++) {
                if (esCampoExcluido(nombres[i]) || !esValorSeguro(valores[i])) {
                    continue;
                }
                snapshot.put(nombres[i], normalizar(valores[i]));
            }
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception | LinkageError e) {
            return null;
        }
    }

    private boolean esCampoExcluido(String nombre) {
        String n = nombre.toLowerCase();
        // clubId es igual en todas las filas del club: solo hace ruido en el detalle.
        return n.equals("clubid") || FRAGMENTOS_CAMPO_EXCLUIDO.stream().anyMatch(n::contains);
    }

    // Valores ya cargados (no tipos declarados): null o escalares. Las relaciones llegan como entidades/colecciones.
    private boolean esValorSeguro(Object v) {
        return v == null || v instanceof String || v instanceof Number || v instanceof Boolean
                || v instanceof Enum<?> || v instanceof Temporal;
    }

    // Jackson sin modulo java.time NO sabe escribir LocalDate/LocalDateTime y lanza excepcion: serializarSnapshot
    // la tragaba y devolvia null, asi que toda entidad con una fecha quedaba auditada SIN detalle. Se pasan a texto.
    private Object normalizar(Object v) {
        if (v instanceof Temporal || v instanceof Enum<?>) {
            return v instanceof Enum<?> e ? e.name() : v.toString();
        }
        return v;
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
                if (java.lang.reflect.Modifier.isStatic(campo.getModifiers())
                        || esCampoExcluido(campo.getName()) || !esTipoSeguro(campo.getType())) {
                    continue;
                }
                campo.setAccessible(true);
                snapshot.put(campo.getName(), normalizar(campo.get(entidad)));
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