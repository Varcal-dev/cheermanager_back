package com.varcal.cheermanager.DTO;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.DTO.Org_dep.InscripcionActualizacionDTO;
import com.varcal.cheermanager.config.GlobalExceptionHandler;

import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * SEC-02: PUT /api/inscripciones/{id} recibe un DTO propio en vez de la entidad Inscripcion. Misma forma JSON que ya
 * envia el front (los planes como objeto con id); lo que sobre en el cuerpo se ignora.
 */
class ValidacionInscripcionActualizacionTest {

    // ---------- DTO ----------

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

    private static InscripcionActualizacionDTO valido() {
        InscripcionActualizacionDTO d = new InscripcionActualizacionDTO();
        d.setFechaInscripcion(LocalDate.of(2026, 11, 27));
        d.setEstado("Activa");
        return d;
    }

    @Test
    void exige_fecha_de_inscripcion_y_estado() {
        assertEquals(Set.of("fechaInscripcion", "estado"), campos(new InscripcionActualizacionDTO()));
        assertTrue(campos(valido()).isEmpty()); // planes y fecha de vencimiento son opcionales
    }

    @Test
    void el_estado_solo_admite_Activa_o_Inactiva() {
        InscripcionActualizacionDTO d = valido();
        d.setEstado("Inactiva");
        assertTrue(campos(d).isEmpty());
        d.setEstado("Suspendida");
        assertEquals(Set.of("estado"), campos(d));
        d.setEstado(" ");
        assertTrue(campos(d).contains("estado"));
    }

    @Test
    void un_plan_presente_exige_su_id_y_ausente_no_exige_nada() {
        InscripcionActualizacionDTO d = valido();
        d.setPlanPago(new InscripcionActualizacionDTO.PlanRef());
        d.setPlanMensualidad(new InscripcionActualizacionDTO.PlanRef());
        assertEquals(Set.of("planPago.id", "planMensualidad.id"), campos(d));
        d.getPlanPago().setId(3);
        d.getPlanMensualidad().setId(5);
        assertTrue(campos(d).isEmpty());
    }

    // ---------- HTTP con el JSON real del front ----------

    @RestController
    static class ControladorPrueba {
        InscripcionActualizacionDTO recibido;

        @PutMapping("/t/inscripciones/{id}")
        public String actualizar(@PathVariable Integer id, @Valid @RequestBody InscripcionActualizacionDTO cambios) {
            recibido = cambios;
            return "ok";
        }
    }

    private ControladorPrueba controlador;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        controlador = new ControladorPrueba();
        mvc = MockMvcBuilders.standaloneSetup(controlador)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void el_cuerpo_del_modal_de_edicion_pasa() throws Exception {
        // EditarInscripcionModal.tsx
        mvc.perform(put("/t/inscripciones/9").contentType(MediaType.APPLICATION_JSON)
                .content("{\"planPago\":{\"id\":3},\"planMensualidad\":{\"id\":5},\"fechaInscripcion\":\"2026-11-01\","
                        + "\"fechaVencimiento\":\"2026-12-01\",\"estado\":\"Activa\"}"))
                .andExpect(status().isOk());
        assertEquals(3, controlador.recibido.getPlanPago().getId());
        assertEquals(5, controlador.recibido.getPlanMensualidad().getId());
        assertEquals(LocalDate.of(2026, 12, 1), controlador.recibido.getFechaVencimiento());
    }

    @Test
    void el_cuerpo_de_la_matricula_con_mensualidad_null_pasa() throws Exception {
        // useMatriculaState.ts: planMensualidad null cuando no se elige
        mvc.perform(put("/t/inscripciones/9").contentType(MediaType.APPLICATION_JSON)
                .content("{\"planPago\":{\"id\":3},\"planMensualidad\":null,\"fechaInscripcion\":\"2026-11-27\","
                        + "\"fechaVencimiento\":\"2026-12-27\",\"estado\":\"Activa\"}"))
                .andExpect(status().isOk());
        assertEquals(null, controlador.recibido.getPlanMensualidad());
    }

    @Test
    void lo_que_sobra_en_el_cuerpo_se_ignora() throws Exception {
        // antes llegaba al endpoint como entidad Inscripcion: ahora deportista, temporada y clubId no existen aqui
        mvc.perform(put("/t/inscripciones/9").contentType(MediaType.APPLICATION_JSON)
                .content("{\"planPago\":{\"id\":3},\"fechaInscripcion\":\"2026-11-01\",\"estado\":\"Activa\","
                        + "\"clubId\":99,\"id\":1234,\"deportista\":{\"id\":77},\"temporada\":{\"id\":2}}"))
                .andExpect(status().isOk());
    }

    @Test
    void sin_fecha_de_inscripcion_es_400_con_mensaje_claro() throws Exception {
        mvc.perform(put("/t/inscripciones/9").contentType(MediaType.APPLICATION_JSON)
                .content("{\"planPago\":{\"id\":3},\"estado\":\"Activa\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("fechaInscripcion: ")));
    }

    @Test
    void estado_invalido_y_plan_sin_id_son_400() throws Exception {
        mvc.perform(put("/t/inscripciones/9").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fechaInscripcion\":\"2026-11-01\",\"estado\":\"Suspendida\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("estado: ")));
        mvc.perform(put("/t/inscripciones/9").contentType(MediaType.APPLICATION_JSON)
                .content("{\"planPago\":{},\"fechaInscripcion\":\"2026-11-01\",\"estado\":\"Activa\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("planPago.id: ")));
    }
}
