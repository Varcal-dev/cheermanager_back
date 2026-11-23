package com.varcal.cheermanager.DTO;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.DTO.Comunes.Crear;
import com.varcal.cheermanager.DTO.Persona.EntrenadorDTO;
import com.varcal.cheermanager.config.GlobalExceptionHandler;

import jakarta.validation.Valid;

/**
 * SEC-02 fase 2: el mismo DTO exige campos al crear (@Validated(Crear.class)) y admite PATCH parcial (@Valid).
 * Controlador minimo con las mismas anotaciones que EntrenadoresController.
 */
class ValidacionGrupoCrearHttpTest {

    @RestController
    static class ControladorPrueba {
        @PostMapping("/t/entrenadores")
        public String crear(@Validated(Crear.class) @RequestBody EntrenadorDTO dto) {
            return "ok";
        }

        @PatchMapping("/t/entrenadores/1")
        public String modificar(@Valid @RequestBody EntrenadorDTO dto) {
            return "ok";
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ControladorPrueba())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void alta_sin_nombre_es_400() throws Exception {
        mvc.perform(post("/t/entrenadores").contentType(MediaType.APPLICATION_JSON).content("{\"telefono\":\"300\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(": ")));
    }

    @Test
    void alta_completa_pasa() throws Exception {
        mvc.perform(post("/t/entrenadores").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Luis\",\"apellidos\":\"Gomez\",\"generoId\":1,\"estadoId\":1,"
                        + "\"fechaContratacion\":\"2026-01-01\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void patch_parcial_pasa_pero_formato_invalido_es_400() throws Exception {
        mvc.perform(patch("/t/entrenadores/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefono\":\"3001234567\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/t/entrenadores/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fechaNacimiento\":\"2999-01-01\"}"))
                .andExpect(status().isBadRequest());
    }
}
