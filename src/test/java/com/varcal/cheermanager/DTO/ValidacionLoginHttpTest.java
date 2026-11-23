package com.varcal.cheermanager.DTO;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Controller.Auth.AuthController.LoginRequest;
import com.varcal.cheermanager.config.GlobalExceptionHandler;

import jakarta.validation.Valid;

/**
 * SEC-02 fase 3: un login con campos ausentes es 400 (antes un NullPointerException = 500).
 * Controlador minimo con la misma firma que AuthController.login, sin servicios ni seguridad.
 */
class ValidacionLoginHttpTest {

    @RestController
    static class ControladorPrueba {
        @PostMapping("/t/login")
        public String login(@Valid @RequestBody LoginRequest r) {
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
    void cuerpo_vacio_o_sin_clave_es_400() throws Exception {
        mvc.perform(post("/t/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        mvc.perform(post("/t/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"a@b.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cuerpo_completo_pasa() throws Exception {
        mvc.perform(post("/t/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@b.com\",\"password\":\"clave\"}"))
                .andExpect(status().isOk());
    }
}
