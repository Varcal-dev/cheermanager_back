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

    @BeforeEach
    void permisosBase() {
        for (String n : List.of("ver_nivel", "crear_nivel", "modificar_nivel", "eliminar_nivel", "ASIGNAR_PERMISO",
                "ver_roles", "gestionar_roles")) {
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
        MvcResult r = mvc.perform(post("/api/clubes").header("Authorization", "Bearer " + superToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("nombre", nombre, "adminUsername", "admin_" + UUID.randomUUID().toString().substring(0, 8),
                        "adminEmail", adminEmail, "adminPassword", PASS))))
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
}
