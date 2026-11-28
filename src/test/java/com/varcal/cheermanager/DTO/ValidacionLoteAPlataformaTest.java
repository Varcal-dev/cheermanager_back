package com.varcal.cheermanager.DTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Service.Clubes.ClubService.CrearClubRequest;
import com.varcal.cheermanager.Service.Clubes.PlataformaService.NuevoUsuarioRequest;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/** SEC-02 fase 5A (plataforma y SaaS): crear club y crear usuario de un club. Largos segun las columnas. */
class ValidacionLoteAPlataformaTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void iniciar() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrar() {
        fabrica.close();
    }

    private static Set<String> campos(Object o) {
        Set<String> r = new TreeSet<>();
        validador.validate(o).forEach(v -> r.add(v.getPropertyPath().toString()));
        return r;
    }

    private static String x(int n) {
        return "x".repeat(n);
    }

    @Test
    void crear_club_exige_nombre_y_datos_del_administrador() {
        assertEquals(Set.of("nombre", "adminUsername", "adminEmail"),
                campos(new CrearClubRequest(null, null, null, null, null, null)));
        assertEquals(Set.of("nombre", "adminUsername", "adminEmail"),
                campos(new CrearClubRequest(" ", null, " ", " ", "clave-larga-123", null)));
        assertTrue(campos(new CrearClubRequest("Ratels", null, "admin", "a@b.co", "clave-larga-123", true)).isEmpty());
    }

    @Test
    void crear_club_respeta_las_columnas_y_el_limite_de_bcrypt() {
        assertEquals(Set.of("nombre", "slug", "adminUsername", "adminEmail", "adminPassword"),
                campos(new CrearClubRequest(x(121), x(61), x(256), x(256), x(73), null)));
        assertEquals(Set.of("adminPassword"), campos(new CrearClubRequest("Ratels", null, "admin", "a@b.co", "corta", null)));
        assertTrue(campos(new CrearClubRequest(x(120), x(60), "admin", "a@b.co", x(72), null)).isEmpty());
    }

    @Test
    void nuevo_usuario_exige_usuario_correo_y_rol() {
        assertEquals(Set.of("username", "email", "rolId"), campos(new NuevoUsuarioRequest(null, null, null)));
        assertTrue(campos(new NuevoUsuarioRequest("ana", "ana@club.co", 3)).isEmpty());
        assertEquals(Set.of("username", "email"), campos(new NuevoUsuarioRequest(x(256), x(256), 3)));
    }
}
