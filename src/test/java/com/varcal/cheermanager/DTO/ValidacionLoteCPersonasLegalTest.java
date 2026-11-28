package com.varcal.cheermanager.DTO;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.varcal.cheermanager.Controller.Consentimientos.ConsentimientoController;
import com.varcal.cheermanager.Controller.Historiales.HistorialNivelDeportistaController;
import com.varcal.cheermanager.Controller.Historiales.HistorialPlanesDeportistaController;
import com.varcal.cheermanager.Controller.Persona.AsistenciaPersonalController;
import com.varcal.cheermanager.Controller.Portal.PortalController;
import com.varcal.cheermanager.DTO.Persona.PersonaDTO;
import com.varcal.cheermanager.Models.Consentimientos.Consentimiento.Tipo;
import com.varcal.cheermanager.Service.Consentimientos.ConsentimientoService.OtorgarRequest;
import com.varcal.cheermanager.config.GlobalExceptionHandler;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * SEC-02 fase 5C (personas y legal): reglas de los DTO nuevos y rechazo 400 en los endpoints que antes recibian Map.
 * Los casos 400 no llegan al servicio, por eso los controladores se crean sin dependencias.
 */
class ValidacionLoteCPersonasLegalTest {

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

    // ---------- DTO ----------

    @Test
    void consentimiento_exige_persona_tipo_y_version_y_respeta_los_largos_de_columna() {
        assertEquals(Set.of("personaId", "tipo", "versionTexto"), campos(new OtorgarRequest(null, null, null, "PAPEL", null, null)));
        assertTrue(campos(new OtorgarRequest(1, Tipo.values()[0], "v1", "PAPEL", null, null)).isEmpty());
        assertEquals(Set.of("versionTexto", "canal", "observaciones"),
                campos(new OtorgarRequest(1, Tipo.values()[0], x(41), x(31), null, x(301))));
    }

    @Test
    void persona_exige_nombre_y_limita_los_textos() {
        PersonaDTO d = new PersonaDTO();
        assertEquals(Set.of("nombre", "apellidos"), campos(d));
        d.setNombre(" ");
        assertEquals(Set.of("nombre", "apellidos"), campos(d));
        d.setNombre("Ana");
        d.setApellidos("Perez");
        assertTrue(campos(d).isEmpty());
        d.setDireccion(x(256));
        d.setTelefono(x(256));
        d.setApellidos(x(256));
        assertEquals(Set.of("direccion", "telefono", "apellidos"), campos(d));
    }

    // ---------- HTTP: los endpoints que recibian Map ----------

    private static MockMvc mvc(Object... controladores) {
        return MockMvcBuilders.standaloneSetup(controladores).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void exigencia_de_salud_con_cuerpo_vacio_ya_no_desactiva_en_silencio() throws Exception {
        mvc(new ConsentimientoController(null))
                .perform(put("/api/consentimientos/exigencia-salud").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("activa: ")));
    }

    @Test
    void historial_de_nivel_sin_ids_es_400_con_el_campo() throws Exception {
        mvc(new HistorialNivelDeportistaController())
                .perform(post("/api/historial-nivel").contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"ascenso\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("deportistaId: ")))
                .andExpect(jsonPath("$.message", containsString("nivelId: ")));
    }

    @Test
    void historial_de_planes_sin_ids_o_con_motivo_largo_es_400() throws Exception {
        MockMvc m = mvc(new HistorialPlanesDeportistaController());
        m.perform(post("/api/historial-planes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("planPagoId: ")));
        m.perform(post("/api/historial-planes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"deportistaId\":1,\"planPagoId\":2,\"motivoCambio\":\"" + x(256) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("motivoCambio: ")));
    }

    @Test
    void escaneo_sin_codigo_y_solicitud_del_portal_sin_temporada_son_400() throws Exception {
        mvc(new AsistenciaPersonalController())
                .perform(post("/api/asistencia-personal/escaneo").contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("codigo: ")));
        mvc(new PortalController(null))
                .perform(post("/api/portal/deportistas/5/solicitudes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("temporadaId: ")));
    }
}
