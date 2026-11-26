package com.varcal.cheermanager.security;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * PERF-02: el cache de autorizacion solo es seguro si TODO cambio que afecta a la existencia, el rol o los permisos de
 * un usuario lo invalida. Este test escanea el codigo: cada metodo de Service/Controller que escribe usuarios, roles o
 * permisos debe llamar a AutorizacionCache.olvidar..., o estar en la lista de excepciones de abajo con su motivo.
 * Si falla por una escritura nueva: llama a AutorizacionCache.olvidarUsuario / olvidarRolesDelClubActual /
 * olvidarTodo, o (si de verdad no afecta a la existencia ni a los permisos) anadela a EXCEPCIONES explicando por que.
 */
class InvalidacionAutorizacionArquitecturaTest {

    /** "Clase#metodo" -> por que NO hace falta invalidar. */
    private static final Map<String, String> EXCEPCIONES = Map.ofEntries(
            Map.entry("AuthService#authenticate", "solo contadores de intentos y ultimo acceso"),
            Map.entry("AuthService#forgotPassword", "solo el token de recuperacion de clave"),
            Map.entry("AuthService#resetPassword", "solo la clave y el desbloqueo"),
            Map.entry("AuthService#changePassword", "solo la clave"),
            Map.entry("ClubService#sembrar", "club nuevo: aun no hay entradas de ese club en el cache"),
            Map.entry("ClubService#nuevoRol", "club nuevo: aun no hay entradas de ese club en el cache"),
            Map.entry("PlataformaService#crearUsuario", "usuario nuevo: los usuarios inexistentes nunca se guardan"),
            Map.entry("PlataformaService#claveTemporal", "solo la clave"),
            Map.entry("AcudienteService#crearAcceso", "usuario nuevo: los usuarios inexistentes nunca se guardan"),
            Map.entry("PersonaService#registrarUsuario", "usuario nuevo: los usuarios inexistentes nunca se guardan"),
            Map.entry("PersonaService#restablecerClaveTemporal", "solo la clave"),
            Map.entry("PersonaService#vincularDeportistaConUsuario", "solo cambia la persona vinculada, no el rol ni el estado"),
            Map.entry("PersonaService#desvinculcarDeportistaDeUsuario", "solo cambia la persona vinculada, no el rol ni el estado"),
            Map.entry("UserController#actualizarMiPerfil", "datos de perfil propios (correo, nombre...); no toca rol ni activo"));

    private static final Pattern ESCRITURA = Pattern.compile(
            "\\b(userRepository|usuarioRepository|rolRepository|permisoRepository)\\."
                    + "(save|saveAll|saveAndFlush|delete|deleteById|deleteAll)\\(");
    private static final Pattern METODO = Pattern.compile(
            "^\\s*(?:public|private|protected)\\s+[\\w<>\\[\\],.?\\s]+?\\s+(\\w+)\\s*\\(");

    private record Escritura(String clave, String archivo, int linea) {
    }

    @Test
    void todaEscrituraDeUsuariosRolesOPermisosInvalidaElCache() throws IOException {
        List<Escritura> sinInvalidar = new ArrayList<>();
        java.util.Set<String> excepcionesUsadas = new java.util.HashSet<>();
        int totalEscrituras = 0;

        for (Path archivo : archivosFuente()) {
            List<String> lineas = Files.readAllLines(archivo);
            String clase = archivo.getFileName().toString().replace(".java", "");

            // Posicion de inicio de cada metodo, para saber a cual pertenece cada linea.
            List<int[]> inicios = new ArrayList<>();   // {linea, indiceDeNombre}
            List<String> nombres = new ArrayList<>();
            for (int i = 0; i < lineas.size(); i++) {
                Matcher m = METODO.matcher(lineas.get(i));
                if (m.find()) {
                    inicios.add(new int[] { i });
                    nombres.add(m.group(1));
                }
            }
            for (int i = 0; i < lineas.size(); i++) {
                if (!ESCRITURA.matcher(lineas.get(i)).find()) continue;
                totalEscrituras++;
                int m = -1;
                for (int k = 0; k < inicios.size(); k++) if (inicios.get(k)[0] <= i) m = k;
                assertTrue(m >= 0, "No se pudo ubicar el metodo de " + archivo + ":" + (i + 1));
                int fin = m + 1 < inicios.size() ? inicios.get(m + 1)[0] : lineas.size();
                String cuerpo = String.join("\n", lineas.subList(inicios.get(m)[0], fin));
                String clave = clase + "#" + nombres.get(m);
                if (cuerpo.contains("AutorizacionCache.")) continue;
                if (EXCEPCIONES.containsKey(clave)) {
                    excepcionesUsadas.add(clave);
                    continue;
                }
                sinInvalidar.add(new Escritura(clave, archivo.toString(), i + 1));
            }
        }

        assertTrue(totalEscrituras > 20, "el escaneo debe encontrar las escrituras conocidas (vio " + totalEscrituras + ")");
        assertTrue(sinInvalidar.isEmpty(),
                "Escrituras de usuarios/roles/permisos SIN invalidar el cache de autorizacion (PERF-02): " + sinInvalidar);

        // La lista de excepciones no se pudre: cada entrada debe seguir correspondiendo a una escritura real.
        for (String clave : EXCEPCIONES.keySet()) {
            assertTrue(excepcionesUsadas.contains(clave), "Excepcion obsoleta (ya no hay tal escritura): " + clave);
        }
    }

    private static List<Path> archivosFuente() throws IOException {
        List<Path> r = new ArrayList<>();
        for (String carpeta : new String[] { "Service", "Controller" }) {
            Path base = Path.of("src/main/java/com/varcal/cheermanager", carpeta);
            assertTrue(Files.isDirectory(base), "no existe " + base + " (el test debe ejecutarse desde la raiz del proyecto)");
            try (Stream<Path> s = Files.walk(base)) {
                s.filter(p -> p.toString().endsWith(".java")).forEach(r::add);
            }
        }
        return r;
    }
}
