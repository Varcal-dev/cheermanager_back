package com.varcal.cheermanager.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * TEN-01: el filtro {@code @TenantId} de Hibernate solo actua sobre HQL/JPA. NO se aplica a SQL nativo ni a
 * {@code JdbcTemplate}: cada sentencia que toque una tabla de club debe filtrar {@code club_id} a mano, o devuelve o
 * modifica datos de todos los clubes.
 *
 * <p>Este test recorre el codigo fuente (sin Spring ni base de datos, corre en cualquier maquina) y falla si encuentra
 * una sentencia SQL que toca una tabla de club y no menciona {@code club_id}. Las excepciones intencionales van en
 * {@link #LISTA_BLANCA}, cada una con su motivo; una entrada que ya no corresponde a ninguna sentencia tambien falla,
 * para que la lista no se llene de basura.
 *
 * <p>Alcance (nivel 1): comprueba que la SENTENCIA mencione {@code club_id}, no que cada tabla de un JOIN este atada
 * al club. Un {@code JOIN personas per ON per.id = f.persona_id} sin {@code club_id} no se detecta si la tabla
 * principal si filtra. Tampoco ve SQL armado en sentencias Java distintas ({@code sql += ...}); si aparece uno, el test
 * lo marca y se documenta en la lista blanca o se junta el SQL en una sola sentencia.
 */
class SqlConClubIdTest {

    private static final Path JAVA = Path.of("src/main/java");
    private static final Path MIGRACIONES = Path.of("src/main/resources/db/migration");

    /** Excepciones intencionales: archivo (sufijo) + inicio de la sentencia normalizada + motivo. */
    record Permitida(String archivo, String inicio, String motivo) {
    }

    static final List<Permitida> LISTA_BLANCA = List.of(
            new Permitida("Tenant/TenantLookupRepository.java", "SELECT COUNT(*) FROM usuarios WHERE username",
                    "Unicidad GLOBAL de username (diseno: es unico en toda la plataforma); solo cuenta, no devuelve datos."),
            new Permitida("Tenant/TenantLookupRepository.java", "SELECT COUNT(*) FROM usuarios WHERE email",
                    "Unicidad GLOBAL de email (diseno: es unico en toda la plataforma); solo cuenta, no devuelve datos."),
            new Permitida("Service/Clubes/WompiService.java", "UPDATE wompi_transacciones",
                    "Actualiza por id o referencia de una fila ya localizada con la firma del webhook validada."),
            new Permitida("Models/Auth/Usuario.java", "UPDATE usuarios SET activo = false WHERE id = ?",
                    "@SQLDelete: deleteById carga primero la entidad con el filtro del club y borra por su clave primaria."));

    // ---------------------------------------------------------------- pruebas

    @Test
    void ningunaSentenciaSqlDeClubOlvidaClubId() throws IOException {
        Set<String> tablas = tablasDeClub();
        List<String> sinPermiso = new ArrayList<>();
        for (Hallazgo h : hallazgos(tablas)) {
            if (!h.permitida()) {
                sinPermiso.add(h.archivo() + ":" + h.linea() + "  tablas " + h.tablas() + "  -> " + resumen(h.sql()));
            }
        }
        assertTrue(sinPermiso.isEmpty(), "SQL que toca tablas de club SIN club_id (fuga entre clubes). Agrega el filtro o, "
                + "si es intencional, registralo en LISTA_BLANCA con su motivo:\n  " + String.join("\n  ", sinPermiso));
    }

    @Test
    void laListaBlancaNoTieneEntradasMuertas() throws IOException {
        Set<String> tablas = tablasDeClub();
        List<Hallazgo> todos = hallazgos(tablas);
        List<String> muertas = new ArrayList<>();
        for (Permitida p : LISTA_BLANCA) {
            boolean usada = todos.stream().anyMatch(h -> h.permitida() && coincide(p, h.archivo(), h.sql()));
            if (!usada) {
                muertas.add(p.archivo() + " :: " + p.inicio());
            }
        }
        assertTrue(muertas.isEmpty(), "Entradas de LISTA_BLANCA que ya no corresponden a ninguna sentencia (borrarlas):\n  "
                + String.join("\n  ", muertas));
    }

    /** Si el escaner dejara de encontrar SQL (cambio de formato, ruta mal), el test de arriba pasaria en vacio. */
    @Test
    void elEscanerEncuentraLasSentenciasReales() throws IOException {
        Set<String> tablas = tablasDeClub();
        int sqlDeClub = 0;
        int conClubId = 0;
        try (Stream<Path> archivos = Files.walk(JAVA)) {
            for (Path p : (Iterable<Path>) archivos.filter(f -> f.toString().endsWith(".java"))::iterator) {
                for (Sentencia s : sentencias(Files.readString(p))) {
                    if (!esSql(s.texto())) {
                        continue;
                    }
                    if (!tablasTocadas(s.texto(), tablas).isEmpty()) {
                        sqlDeClub++;
                        if (tieneClubId(s.texto())) {
                            conClubId++;
                        }
                    }
                }
            }
        }
        assertTrue(sqlDeClub >= 40, "Esperaba >= 40 sentencias SQL de tablas de club y encontre " + sqlDeClub);
        assertTrue(conClubId >= 40, "Esperaba >= 40 sentencias con club_id y encontre " + conClubId);
    }

    @Test
    void detectaUnaSentenciaSinClubId() {
        Set<String> tablas = Set.of("facturas", "personas");
        String malo = """
                class X {
                    List<Object[]> todas() {
                        return em.createNativeQuery("SELECT f.id FROM facturas f " + "WHERE f.anulada = 0").getResultList();
                    }
                }
                """;
        List<Sentencia> sql = sentencias(malo).stream().filter(s -> esSql(s.texto())).toList();
        assertEquals(1, sql.size());
        assertFalse(tablasTocadas(sql.get(0).texto(), tablas).isEmpty());
        assertFalse(tieneClubId(sql.get(0).texto()), "una sentencia sin club_id debe marcarse");

        String bueno = """
                class X {
                    @Query(value = \"\"\"
                            SELECT f.id FROM facturas f
                            JOIN personas p ON p.id = f.persona_id
                            WHERE f.club_id = :club
                            \"\"\", nativeQuery = true)
                    List<Object[]> propias();
                }
                """;
        List<Sentencia> ok = sentencias(bueno).stream().filter(s -> esSql(s.texto())).toList();
        assertEquals(1, ok.size());
        assertEquals(Set.of("facturas", "personas"), tablasTocadas(ok.get(0).texto(), tablas));
        assertTrue(tieneClubId(ok.get(0).texto()));
    }

    @Test
    void jpqlYComentariosNoSeConfundenConSql() {
        Set<String> tablas = Set.of("auditoria", "facturas");
        // JPQL: el nombre de la entidad lleva mayuscula inicial, no es una tabla
        String jpql = "class X { @Query(\"SELECT DISTINCT a.tablaAfectada FROM Auditoria a\") List<String> t(); }";
        for (Sentencia s : sentencias(jpql)) {
            if (esSql(s.texto())) {
                assertTrue(tablasTocadas(s.texto(), tablas).isEmpty());
            }
        }
        // un SQL dentro de un comentario no cuenta
        String comentario = "class X { // SELECT * FROM facturas\n /* SELECT * FROM facturas */ int a; }";
        assertTrue(sentencias(comentario).stream().noneMatch(s -> esSql(s.texto())));
    }

    @Test
    void lasTablasDeClubSeLeenDeEntidadesYMigraciones() throws IOException {
        Set<String> tablas = tablasDeClub();
        // entidades con @TenantId
        assertTrue(tablas.containsAll(Set.of("facturas", "pagos", "personas", "deportistas", "niveles", "inscripciones")));
        // tablas de plataforma con club_id que no son entidad (solo se ven en las migraciones)
        assertTrue(tablas.containsAll(Set.of("wompi_transacciones", "suscripciones_pagos", "clubes_caracteristicas")));
        // catalogos globales: no se exige club_id
        assertFalse(tablas.contains("generos"));
        assertFalse(tablas.contains("planes_saas"));
        assertFalse(tablas.contains("permisos"));
    }

    // ------------------------------------------------------------ escaner

    record Sentencia(int linea, String texto) {
    }

    record Hallazgo(String archivo, int linea, Set<String> tablas, String sql, boolean permitida) {
    }

    private static List<Hallazgo> hallazgos(Set<String> tablas) throws IOException {
        List<Hallazgo> out = new ArrayList<>();
        try (Stream<Path> archivos = Files.walk(JAVA)) {
            for (Path p : (Iterable<Path>) archivos.filter(f -> f.toString().endsWith(".java")).sorted()::iterator) {
                String rel = JAVA.relativize(p).toString().replace('\\', '/');
                // el paquete raiz (com/varcal/cheermanager/) se quita para que la lista blanca sea corta
                String corto = rel.replaceFirst("^com/varcal/cheermanager/", "");
                for (Sentencia s : sentencias(Files.readString(p))) {
                    if (!esSql(s.texto())) {
                        continue;
                    }
                    Set<String> tocadas = tablasTocadas(s.texto(), tablas);
                    if (tocadas.isEmpty() || tieneClubId(s.texto())) {
                        continue;
                    }
                    boolean ok = LISTA_BLANCA.stream().anyMatch(w -> coincide(w, corto, s.texto()));
                    out.add(new Hallazgo(corto, s.linea(), tocadas, s.texto(), ok));
                }
            }
        }
        return out;
    }

    private static boolean coincide(Permitida p, String archivo, String sql) {
        return archivo.endsWith(p.archivo()) && normalizar(sql).startsWith(p.inicio());
    }

    private static final Pattern ES_SQL = Pattern.compile("^\\s*(SELECT|UPDATE|DELETE|INSERT|WITH)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIENE_ORIGEN = Pattern.compile("\\b(FROM|INTO|UPDATE)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TABLA = Pattern.compile("\\b(?:FROM|JOIN|UPDATE|INTO)\\s+`?([a-z_][a-z0-9_]*)`?", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLUB_ID = Pattern.compile("\\bclub_id\\b", Pattern.CASE_INSENSITIVE);

    static boolean esSql(String texto) {
        return ES_SQL.matcher(texto).find() && TIENE_ORIGEN.matcher(texto).find();
    }

    static boolean tieneClubId(String texto) {
        return CLUB_ID.matcher(texto).find();
    }

    /**
     * Tablas de club que toca la sentencia. El nombre se compara tal cual esta escrito (minusculas con guion bajo), asi
     * "FROM Auditoria" (JPQL, nombre de entidad) no se confunde con la tabla "auditoria".
     */
    static Set<String> tablasTocadas(String texto, Set<String> tablasDeClub) {
        Set<String> out = new TreeSet<>();
        Matcher m = TABLA.matcher(texto);
        while (m.find()) {
            String t = m.group(1);
            if (tablasDeClub.contains(t)) {
                out.add(t);
            }
        }
        return out;
    }

    private static String normalizar(String s) {
        return s.replaceAll("\\s+", " ").trim();
    }

    private static String resumen(String sql) {
        String n = normalizar(sql);
        return n.length() > 110 ? n.substring(0, 110) + "..." : n;
    }

    /**
     * Separa el codigo en "sentencias Java" (hasta el siguiente ; { o }) y devuelve, de cada una, los literales de texto
     * unidos (incluidos bloques de texto y cadenas concatenadas con +). Ignora comentarios y literales de caracter.
     */
    static List<Sentencia> sentencias(String src) {
        List<Sentencia> out = new ArrayList<>();
        List<String> buf = new ArrayList<>();
        int i = 0;
        int n = src.length();
        int linea = 1;
        int inicio = 1;
        while (i < n) {
            char c = src.charAt(i);
            if (c == '\n') {
                linea++;
            }
            if (src.startsWith("//", i)) {
                int j = src.indexOf('\n', i);
                i = j < 0 ? n : j;
                continue;
            }
            if (src.startsWith("/*", i)) {
                int j = src.indexOf("*/", i + 2);
                j = j < 0 ? n : j + 2;
                linea += count(src, i, j);
                i = j;
                continue;
            }
            if (src.startsWith("\"\"\"", i)) {
                int j = src.indexOf("\"\"\"", i + 3);
                while (j > 0 && src.charAt(j - 1) == '\\') {
                    j = src.indexOf("\"\"\"", j + 1);
                }
                if (j < 0) {
                    j = n - 3;
                }
                if (buf.isEmpty()) {
                    inicio = linea;
                }
                buf.add(src.substring(i + 3, j));
                linea += count(src, i, j + 3);
                i = j + 3;
                continue;
            }
            if (c == '"') {
                int j = i + 1;
                while (j < n && src.charAt(j) != '"') {
                    j += src.charAt(j) == '\\' ? 2 : 1;
                }
                if (buf.isEmpty()) {
                    inicio = linea;
                }
                buf.add(src.substring(i + 1, Math.min(j, n)));
                i = j + 1;
                continue;
            }
            if (c == '\'') {
                int j = i + 1;
                while (j < n && src.charAt(j) != '\'') {
                    j += src.charAt(j) == '\\' ? 2 : 1;
                }
                i = j + 1;
                continue;
            }
            if (c == ';' || c == '{' || c == '}') {
                if (!buf.isEmpty()) {
                    out.add(new Sentencia(inicio, String.join(" ", buf)));
                }
                buf = new ArrayList<>();
            }
            i++;
        }
        if (!buf.isEmpty()) {
            out.add(new Sentencia(inicio, String.join(" ", buf)));
        }
        return out;
    }

    private static int count(String s, int desde, int hasta) {
        int k = 0;
        for (int i = desde; i < Math.min(hasta, s.length()); i++) {
            if (s.charAt(i) == '\n') {
                k++;
            }
        }
        return k;
    }

    // ------------------------------------------------------ tablas de club

    /**
     * Tablas de club = entidades con {@code @TenantId} + tablas cuyo CREATE TABLE tiene la columna club_id (cubre tablas
     * de plataforma que se usan con JdbcTemplate y no son entidad, como wompi_transacciones).
     */
    static Set<String> tablasDeClub() throws IOException {
        Set<String> tablas = new TreeSet<>();
        Pattern tabla = Pattern.compile("@Table\\s*\\(\\s*name\\s*=\\s*\"(\\w+)\"");
        Pattern clase = Pattern.compile("public\\s+(?:class|enum)\\s+(\\w+)");
        try (Stream<Path> archivos = Files.walk(JAVA)) {
            for (Path p : (Iterable<Path>) archivos.filter(f -> f.toString().endsWith(".java"))::iterator) {
                String s = Files.readString(p);
                if (!s.contains("org.hibernate.annotations.TenantId")) {
                    continue;
                }
                Matcher m = tabla.matcher(s);
                if (m.find()) {
                    tablas.add(m.group(1));
                } else {
                    Matcher c = clase.matcher(s);
                    if (c.find()) {
                        tablas.add(c.group(1).replaceAll("(?<!^)([A-Z])", "_$1").toLowerCase());
                    }
                }
            }
        }
        Pattern create = Pattern.compile("CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`?(\\w+)`?\\s*\\((.*?)\\n\\)\\s*(?:ENGINE|;)",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        try (Stream<Path> migraciones = Files.list(MIGRACIONES)) {
            for (Path p : (Iterable<Path>) migraciones.filter(f -> f.toString().endsWith(".sql"))::iterator) {
                Matcher m = create.matcher(Files.readString(p));
                while (m.find()) {
                    if (CLUB_ID.matcher(m.group(2)).find()) {
                        tablas.add(m.group(1));
                    }
                }
            }
        }
        return tablas;
    }
}
