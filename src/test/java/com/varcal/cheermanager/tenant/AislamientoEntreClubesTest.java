package com.varcal.cheermanager.tenant;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.varcal.cheermanager.Models.Auth.Permiso;
import com.varcal.cheermanager.repository.Auth.PermisoRepository;

/**
 * PRUEBA CLAVE DE LA FASE 1: un club jamas ve ni toca datos de otro.
 * Recorre la API real (login -> token con clubId -> endpoints) con dos clubes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.bootstrap.admin-username=superadmin",
        "app.bootstrap.admin-email=root@plataforma.test",
        "app.bootstrap.admin-password=ClaveSuperSegura1" })
class AislamientoEntreClubesTest {

    private static final String PASS = "ClaveClub12345";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PermisoRepository permisoRepository;
    @Autowired com.varcal.cheermanager.repository.Personas.EstadoPersonaRepository estadoPersonaRepository;
    @Autowired com.varcal.cheermanager.repository.Personas.DeportistaRepository deportistaRepository;

    @BeforeEach
    void permisosBase() {
        for (String n : List.of("ver_nivel", "crear_nivel", "modificar_nivel", "eliminar_nivel", "asignar_permiso",
                "ver_roles", "gestionar_roles", "crear_deportista", "ver_deportista",
                "ver_acudientes", "gestionar_acudientes", "ver_temporadas", "gestionar_temporadas",
                "ver_consentimientos", "gestionar_consentimientos", "ver_onboarding", "restablecer_clave_usuario")) {
            boolean existe = permisoRepository.findAll().stream().anyMatch(p -> n.equals(p.getNombre()));
            if (!existe) {
                Permiso p = new Permiso();
                p.setNombre(n);
                permisoRepository.save(p);
            }
        }
    }

    private String login(String email, String password) throws Exception {
        MvcResult r = mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", password)))).andReturn();
        if (r.getResponse().getStatus() != 200) {
            return null;
        }
        return json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private int crearClub(String superToken, String nombre, String adminEmail) throws Exception {
        return crearClub(superToken, nombre, adminEmail, false); // sin plantilla: los tests parten de un club vacio
    }

    private int crearClub(String superToken, String nombre, String adminEmail, boolean plantilla) throws Exception {
        MvcResult r = mvc.perform(post("/api/clubes").header("Authorization", "Bearer " + superToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombre", nombre, "adminUsername", "admin_" + UUID.randomUUID().toString().substring(0, 8),
                        "adminEmail", adminEmail, "adminPassword", PASS, "cargarPlantilla", plantilla))))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("id").asInt();
    }

    @Test
    void unClubNoVeNiTocaLosDatosDeOtro() throws Exception {
        String sfx = UUID.randomUUID().toString().substring(0, 6);
        String superToken = login("root@plataforma.test", "ClaveSuperSegura1");
        assertNotNull(superToken, "el superadmin debe poder entrar");

        int clubA = crearClub(superToken, "Club Alfa " + sfx, "a-" + sfx + "@alfa.test");
        int clubB = crearClub(superToken, "Club Beta " + sfx, "b-" + sfx + "@beta.test");
        assertNotEquals(clubA, clubB);

        String tokenA = login("a-" + sfx + "@alfa.test", PASS);
        String tokenB = login("b-" + sfx + "@beta.test", PASS);
        assertNotNull(tokenA);
        assertNotNull(tokenB);

        // A crea un nivel
        MvcResult creado = mvc.perform(post("/api/niveles").header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Infantil\"}"))
                .andExpect(status().is2xxSuccessful()).andReturn();
        int idA = json.readTree(creado.getResponse().getContentAsString()).get("id").asInt();

        // A lo ve; B NO lo ve en la lista ni por id
        mvc.perform(get("/api/niveles").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/niveles").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/niveles/" + idA).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().is4xxClientError());

        // B no puede modificar ni borrar el nivel de A (IDOR)
        mvc.perform(put("/api/niveles/" + idA).header("Authorization", "Bearer " + tokenB)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Hackeado\"}"))
                .andExpect(status().is4xxClientError());
        mvc.perform(delete("/api/niveles/" + idA).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().is4xxClientError());
        mvc.perform(get("/api/niveles/" + idA).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Infantil"));

        // La unicidad es POR CLUB: B puede tener un nivel con el mismo nombre
        mvc.perform(post("/api/niveles").header("Authorization", "Bearer " + tokenB)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Infantil\"}"))
                .andExpect(status().is2xxSuccessful());

        // Roles: cada club solo ve los suyos (JPQL del conteo filtrado)
        mvc.perform(get("/api/usuario/rol/listar-con-usuarios").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3)); // ADMIN, Entrenador, Deportista

        // Un admin de club NO es superadmin
        mvc.perform(get("/api/clubes").header("Authorization", "Bearer " + tokenA)).andExpect(status().isForbidden());
    }

    @Test
    void unAdminDeClubNoPuedeDarseUnPermisoDePlataforma() throws Exception {
        String sfx = UUID.randomUUID().toString().substring(0, 6);
        String superToken = login("root@plataforma.test", "ClaveSuperSegura1");
        crearClub(superToken, "Club Gamma " + sfx, "g-" + sfx + "@gamma.test");
        String tokenG = login("g-" + sfx + "@gamma.test", PASS);

        Integer idClubes = permisoRepository.findAll().stream()
                .filter(p -> "gestionar_clubes".equals(p.getNombre())).findFirst().orElseThrow().getId();
        // roles del club G
        JsonNode roles = json.readTree(mvc.perform(get("/api/roles").header("Authorization", "Bearer " + tokenG))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        int rolId = roles.get(0).get("id").asInt();

        mvc.perform(post("/api/roles/asignar-permisos").header("Authorization", "Bearer " + tokenG)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("rolId", rolId, "permisoIds", List.of(idClubes)))))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void clubSuspendidoPierdeElAccesoYLoginNoRevelaSiElCorreoExiste() throws Exception {
        String sfx = UUID.randomUUID().toString().substring(0, 6);
        String superToken = login("root@plataforma.test", "ClaveSuperSegura1");
        int club = crearClub(superToken, "Club Delta " + sfx, "d-" + sfx + "@delta.test");
        String token = login("d-" + sfx + "@delta.test", PASS);
        mvc.perform(get("/api/niveles").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        mvc.perform(patch("/api/clubes/" + club + "/activo").header("Authorization", "Bearer " + superToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"activo\":false}")).andExpect(status().isOk());

        // el token existente deja de servir y no se puede volver a entrar
        mvc.perform(get("/api/niveles").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        assertNull(login("d-" + sfx + "@delta.test", PASS));

        // mismo mensaje para correo inexistente y clave mala
        String m1 = mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nadie@nada.test\",\"password\":\"x\"}")).andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String m2 = mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"root@plataforma.test\",\"password\":\"mala\"}")).andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertEquals(m1, m2);
    }

    /** FASE 2: un club no puede apuntar a un nivel de otro club con un id suelto (Integer sin relacion JPA). */
    @Test
    void deportistaNoPuedeUsarUnNivelDeOtroClub() throws Exception {
        String sfx = UUID.randomUUID().toString().substring(0, 6);
        com.varcal.cheermanager.Models.Personas.EstadoPersona estado = estadoPersonaRepository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    com.varcal.cheermanager.Models.Personas.EstadoPersona e = new com.varcal.cheermanager.Models.Personas.EstadoPersona();
                    e.setEstado("Activo");
                    return estadoPersonaRepository.save(e);
                });
        String superToken = login("root@plataforma.test", "ClaveSuperSegura1");
        crearClub(superToken, "Club Delta " + sfx, "d-" + sfx + "@delta.test");
        crearClub(superToken, "Club Epsilon " + sfx, "e-" + sfx + "@epsilon.test");
        String tokenD = login("d-" + sfx + "@delta.test", PASS);
        String tokenE = login("e-" + sfx + "@epsilon.test", PASS);

        int nivelD = json.readTree(mvc.perform(post("/api/niveles").header("Authorization", "Bearer " + tokenD)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Elite\"}"))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString()).get("id").asInt();

        String cuerpo = "{\"nombre\":\"Ana\",\"apellidos\":\"Prueba\",\"numeroDocumento\":\"DOC" + sfx
                + "\",\"estadoId\":" + estado.getId() + ",\"nivelActualId\":" + nivelD + "}";

        // E intenta usar el nivel de D: rechazado y NO se guarda nada
        MvcResult rechazado = mvc.perform(post("/api/deportistas").header("Authorization", "Bearer " + tokenE)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn();
        assertEquals(400, rechazado.getResponse().getStatus(), "debe rechazar un nivel de otro club");
        // y no quedo nada a medias: el listado del club E no contiene a esa persona
        // (si el endpoint de listado exigiera otro permiso, el test lo mostraria como 403)
        MvcResult listado = mvc.perform(get("/api/deportistas").header("Authorization", "Bearer " + tokenE)).andReturn();
        assertFalse(listado.getResponse().getContentAsString().contains("DOC" + sfx),
                "no debe haber quedado ningun deportista a medias");

        // D si puede usar su propio nivel
        mvc.perform(post("/api/deportistas").header("Authorization", "Bearer " + tokenD)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isOk());
    }

    /** FASE 3: acudientes y temporadas son por club; solo una temporada activa; la inscripcion no cruza clubes. */
    @Test
    void acudientesYTemporadasSonPorClub() throws Exception {
        String sfx = UUID.randomUUID().toString().substring(0, 6);
        String superToken = login("root@plataforma.test", "ClaveSuperSegura1");
        crearClub(superToken, "Club Zeta " + sfx, "z-" + sfx + "@zeta.test");
        crearClub(superToken, "Club Eta " + sfx, "h-" + sfx + "@eta.test");
        String tokenZ = login("z-" + sfx + "@zeta.test", PASS);
        String tokenH = login("h-" + sfx + "@eta.test", PASS);

        int acudiente = json.readTree(mvc.perform(post("/api/acudientes").header("Authorization", "Bearer " + tokenZ)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Marta\",\"apellidos\":\"Lopez\",\"numeroDocumento\":\"D" + sfx + "\",\"telefono\":\"3000000000\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asInt();

        // el otro club no lo ve ni por id ni en la lista
        mvc.perform(get("/api/acudientes/" + acudiente).header("Authorization", "Bearer " + tokenH))
                .andExpect(status().is4xxClientError());
        mvc.perform(get("/api/acudientes").header("Authorization", "Bearer " + tokenH))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        // y no puede borrarlo
        mvc.perform(delete("/api/acudientes/" + acudiente).header("Authorization", "Bearer " + tokenH))
                .andExpect(status().is4xxClientError());

        // solo una temporada activa por club
        String cuerpo1 = "{\"nombre\":\"2026\",\"fechaInicio\":\"2026-01-01\",\"fechaFin\":\"2026-12-31\"}";
        String cuerpo2 = "{\"nombre\":\"2027\",\"fechaInicio\":\"2027-01-01\",\"fechaFin\":\"2027-12-31\"}";
        int t1 = json.readTree(mvc.perform(post("/api/temporadas").header("Authorization", "Bearer " + tokenZ)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo1))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asInt();
        int t2 = json.readTree(mvc.perform(post("/api/temporadas").header("Authorization", "Bearer " + tokenZ)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo2))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asInt();
        mvc.perform(patch("/api/temporadas/" + t1 + "/activar").header("Authorization", "Bearer " + tokenZ))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/temporadas/" + t2 + "/activar").header("Authorization", "Bearer " + tokenZ))
                .andExpect(status().isOk());
        mvc.perform(get("/api/temporadas/activa").header("Authorization", "Bearer " + tokenZ))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("2027"));
        // el otro club no puede activar la temporada ajena y no tiene ninguna activa
        mvc.perform(patch("/api/temporadas/" + t1 + "/activar").header("Authorization", "Bearer " + tokenH))
                .andExpect(status().is4xxClientError());
        mvc.perform(get("/api/temporadas/activa").header("Authorization", "Bearer " + tokenH))
                .andExpect(status().isNoContent());
    }

    /** FASE 3: un club nuevo con plantilla arranca con niveles/divisiones y el onboarding lo refleja. */
    @Test
    void plantillaInicialYOnboarding() throws Exception {
        String sfx = UUID.randomUUID().toString().substring(0, 6);
        String superToken = login("root@plataforma.test", "ClaveSuperSegura1");
        crearClub(superToken, "Club Theta " + sfx, "t-" + sfx + "@theta.test", true);
        String token = login("t-" + sfx + "@theta.test", PASS);

        mvc.perform(get("/api/niveles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(6));
        mvc.perform(get("/api/onboarding").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasos[?(@.clave=='niveles')].completo").value(true))
                .andExpect(jsonPath("$.pasos[?(@.clave=='temporada')].completo").value(false));
    }
}
