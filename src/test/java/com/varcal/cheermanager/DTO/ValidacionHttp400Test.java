package com.varcal.cheermanager.DTO;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.DTO.Inventario.ActualizarPrecioDTO;
import com.varcal.cheermanager.Models.Financiero.Descuento;
import com.varcal.cheermanager.config.GlobalExceptionHandler;

import jakarta.validation.Valid;

/**
 * SEC-02 fase 1: un cuerpo invalido llega como 400 con "campo: mensaje" gracias a @Valid +
 * GlobalExceptionHandler, y un cuerpo valido sigue pasando. Se usan controladores minimos que
 * copian la firma real (@Valid @RequestBody) para no depender de servicios ni de seguridad.
 */
class ValidacionHttp400Test {

    @RestController
    static class ControladorPrueba {
        @PostMapping("/t/descuentos")
        public Descuento crear(@Valid @RequestBody Descuento d) {
            return d;
        }

        @PutMapping("/t/variantes/{id}/precio")
        public ActualizarPrecioDTO precio(@PathVariable Integer id, @Valid @RequestBody ActualizarPrecioDTO body) {
            return body;
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
    void descuento_con_porcentaje_500_es_400() throws Exception {
        mvc.perform(post("/t/descuentos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"X\",\"porcentaje\":500,\"fechaInicio\":\"2026-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("porcentaje: ")));
    }

    @Test
    void descuento_valido_pasa() throws Exception {
        mvc.perform(post("/t/descuentos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"X\",\"porcentaje\":15,\"fechaInicio\":\"2026-01-01\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void precio_negativo_es_400_y_cero_pasa() throws Exception {
        mvc.perform(put("/t/variantes/1/precio").contentType(MediaType.APPLICATION_JSON)
                .content("{\"precioVenta\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("precioVenta: ")));
        mvc.perform(put("/t/variantes/1/precio").contentType(MediaType.APPLICATION_JSON)
                .content("{\"precioVenta\":0}"))
                .andExpect(status().isOk());
    }

    @Test
    void precio_ausente_es_400_y_forma_json_original_sigue_valida() throws Exception {
        mvc.perform(put("/t/variantes/1/precio").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/t/variantes/1/precio").contentType(MediaType.APPLICATION_JSON)
                .content("{\"precioVenta\":25000}"))
                .andExpect(status().isOk());
    }
}
