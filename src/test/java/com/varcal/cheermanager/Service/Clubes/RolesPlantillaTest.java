package com.varcal.cheermanager.Service.Clubes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Los roles de plantilla de un club nuevo solo pueden pedir permisos que existan en el catalogo, ninguno de
 * plataforma, y deben llamarse igual que los roles de Ratels (para que las migraciones por nombre de rol apliquen).
 */
class RolesPlantillaTest {

    private static String leer(String recurso) throws Exception {
        try (InputStream in = RolesPlantillaTest.class.getClassLoader().getResourceAsStream(recurso)) {
            assertTrue(in != null, "no se encuentra " + recurso);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Set<String> catalogo() throws Exception {
        String sql = leer("db/migration/V2__datos_iniciales.sql") + "\n"
                + leer("db/migration/V6__portal_acudiente_deportista_y_solicitudes.sql") + "\n"
                + leer("db/migration/V9__recordatorios_mora.sql") + "\n"
                + leer("db/migration/V15__caja_y_conciliacion.sql");
        // Sin comentarios SQL: V2 tiene un ";" dentro de un comentario que cortaba el bloque INSERT.
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

    @Test
    void todoPermisoDeLaPlantillaExisteEnElCatalogo() throws Exception {
        Set<String> catalogo = catalogo();
        Set<String> faltan = new TreeSet<>();
        for (String p : RolesPlantilla.ENTRENADOR_PERMISOS) if (!catalogo.contains(p)) faltan.add(p);
        for (RolesPlantilla.RolBase r : RolesPlantilla.ROLES_ADICIONALES)
            for (String p : r.permisos()) if (!catalogo.contains(p)) faltan.add(r.nombre() + " -> " + p);
        assertTrue(faltan.isEmpty(), "Permisos de plantilla que no existen en el catalogo: " + faltan);
    }

    @Test
    void ningunRolDePlantillaTienePermisosDePlataforma() {
        for (RolesPlantilla.RolBase r : RolesPlantilla.ROLES_ADICIONALES) {
            Set<String> mezcla = new TreeSet<>(r.permisos());
            mezcla.retainAll(RolesPlantilla.SOLO_PLATAFORMA);
            assertTrue(mezcla.isEmpty(), r.nombre() + " tiene permisos de plataforma: " + mezcla);
        }
    }

    @Test
    void losRolesAdicionalesTienenLosNombresDeRatels() {
        assertEquals(Set.of("Director Deportivo", "Coordinador Administrativo", "Asistente", "Tesorero",
                "Encargado de Inventario"),
                RolesPlantilla.ROLES_ADICIONALES.stream().map(RolesPlantilla.RolBase::nombre)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void losRolesDePlantillaTienenLoMismoQueRatels() {
        // Decision del 31-oct-2026: sin restricciones extra por rol. Entrenador y Asistente ven historial medico (como Ratels).
        assertTrue(RolesPlantilla.ENTRENADOR_PERMISOS.contains("ver_historial_medico"));
        assertTrue(RolesPlantilla.ASISTENTE_PERMISOS.contains("ver_historial_medico"));
        // El Coordinador recibe tambien el panel de finanzas (V12 lo agrega en Ratels).
        assertTrue(RolesPlantilla.COORDINADOR_PERMISOS.contains("ver_dash_finanzas"));
    }

    @Test
    void elMenuDeCadaRolCoincideConSuTrabajo() {
        assertTrue(RolesPlantilla.TESORERO_PERMISOS.contains("ver_dash_finanzas"));
        assertTrue(RolesPlantilla.TESORERO_PERMISOS.contains("ver_dash_eventos"));
        assertTrue(RolesPlantilla.COORDINADOR_PERMISOS.contains("ver_dash_personas"));
        assertTrue(RolesPlantilla.DIRECTOR_PERMISOS.contains("ver_dash_eventos"));
        assertTrue(RolesPlantilla.INVENTARIO_PERMISOS.contains("ver_dash_tienda"));
    }
}
