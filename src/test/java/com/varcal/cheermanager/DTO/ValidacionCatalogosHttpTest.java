package com.varcal.cheermanager.DTO;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.DTO.Persona.AsignacionEntrenadorDTO;
import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.config.GlobalExceptionHandler;

import jakarta.validation.Valid;

/**
 * SEC-02 fase 4: una entidad de catalogo sin nombre es 400, y la asignacion de entrenador sigue
 * funcionando sin grupoId en el cuerpo (el controlador lo toma de la ruta). Controlador minimo con las
 * mismas anotaciones que RolController y GrupoEntrenamientoController.
 */
class ValidacionCatalogosHttpTest {

    @RestController
    static class ControladorPrueba {
        @PostMapping("/t/roles")
        public String crearRol(@Valid @RequestBody Rol rol) {
            return "ok";
        }

        @PostMapping("/t/grupos/{grupoId}/asignar-entrenadores")
        public String asignar(@PathVariable int grupoId, @Valid @RequestBody AsignacionEntrenadorDTO dto) {
            dto.setGrupoId(grupoId);
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
    void rol_sin_nombre_es_400_y_con_nombre_pasa() throws Exception {
        mvc.perform(post("/t/roles").contentType(MediaType.APPLICATION_JSON).content("{\"descripcion\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("nombre: ")));
        mvc.perform(post("/t/roles").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Entrenador\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void asignar_entrenador_sin_grupoId_en_el_cuerpo_pasa() throws Exception {
        mvc.perform(post("/t/grupos/7/asignar-entrenadores").contentType(MediaType.APPLICATION_JSON)
                .content("{\"entrenadorId\":4,\"fechaInicio\":\"2026-11-23\",\"rol\":\"Principal\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/t/grupos/7/asignar-entrenadores").contentType(MediaType.APPLICATION_JSON)
                .content("{\"rol\":\"Principal\"}"))
                .andExpect(status().isBadRequest());
    }
}
