package com.varcal.cheermanager.testsupport;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Repositorios falsos (java.lang.reflect.Proxy) para probar los Services sin base de datos ni Mockito.
 * Cada metodo se simula por nombre; si el Service llama a uno que la prueba no simulo, falla con un
 * mensaje claro (asi una prueba nunca pasa "por casualidad" con un null).
 */
public final class Fakes {

    private Fakes() {}

    /** Registro de llamadas hechas a un repositorio falso. */
    public static final class Repo implements InvocationHandler {
        private final Map<String, Function<Object[], Object>> handlers = new HashMap<>();
        private final List<String> llamadas = new ArrayList<>();
        private final List<Object[]> argumentos = new ArrayList<>();

        public Repo on(String metodo, Function<Object[], Object> fn) {
            handlers.put(metodo, fn);
            return this;
        }

        /** Cuantas veces se llamo al metodo. */
        public int veces(String metodo) {
            int n = 0;
            for (String l : llamadas) if (l.equals(metodo)) n++;
            return n;
        }

        /** Argumentos de la ultima llamada al metodo (null si nunca se llamo). */
        public Object[] ultimaLlamada(String metodo) {
            for (int i = llamadas.size() - 1; i >= 0; i--) if (llamadas.get(i).equals(metodo)) return argumentos.get(i);
            return null;
        }

        @Override
        public Object invoke(Object proxy, Method m, Object[] args) {
            if (m.getDeclaringClass() == Object.class) {
                switch (m.getName()) {
                    case "toString": return "RepoFalso";
                    case "hashCode": return System.identityHashCode(proxy);
                    case "equals": return proxy == args[0];
                    default: return null;
                }
            }
            llamadas.add(m.getName());
            argumentos.add(args == null ? new Object[0] : args);
            Function<Object[], Object> fn = handlers.get(m.getName());
            if (fn == null) {
                throw new UnsupportedOperationException("El repositorio falso no simula: " + m.getName());
            }
            return fn.apply(args == null ? new Object[0] : args);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T proxy(Class<T> tipo, Repo repo) {
        return (T) Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[] { tipo }, repo);
    }

    /** Inyecta un valor en un campo (privado o @Autowired) por reflexion, buscando en la jerarquia. */
    public static void inyectar(Object objetivo, String campo, Object valor) {
        Class<?> c = objetivo.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(campo);
                f.setAccessible(true);
                f.set(objetivo, valor);
                return;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("No existe el campo " + campo + " en " + objetivo.getClass());
    }

    /** Compara dinero ignorando la escala (10 == 10.00). */
    public static boolean igual(String esperado, BigDecimal actual) {
        return actual != null && new BigDecimal(esperado).compareTo(actual) == 0;
    }
}
