package com.varcal.cheermanager.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.varcal.cheermanager.Service.ErroresDeServicioFixture;
import com.varcal.cheermanager.Tenant.ErroresDeInfraFixture;

/**
 * SEC-03 fase 1: lo que el cliente recibe de GlobalExceptionHandler. Controladores minimos que lanzan cada tipo de
 * excepcion; las "de negocio" se fabrican en el paquete Service (ErroresDeServicioFixture) y las de infraestructura
 * en Tenant (ErroresDeInfraFixture), porque ErrorPublico decide por el origen de la excepcion.
 */
class GlobalExceptionHandlerSec03Test {

    @RestController
    static class ControladorPrueba {
        // ResponseStatusException
        @GetMapping("/t/rse-409")
        public void rse409() {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El rol Administrador no se puede eliminar.");
        }

        @GetMapping("/t/rse-403")
        public void rse403() {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes modificar a un usuario de mayor jerarquia");
        }

        @GetMapping("/t/rse-sin-motivo")
        public void rseSinMotivo() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        @GetMapping("/t/rse-5xx")
        public void rse5xx() {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "host interno db-1:3306 no responde");
        }

        // IllegalStateException
        @GetMapping("/t/ise-negocio")
        public void iseNegocio() {
            throw ErroresDeServicioFixture.ise("Ya existe una sede con ese nombre");
        }

        @GetMapping("/t/ise-infra")
        public void iseInfra() {
            throw ErroresDeInfraFixture.ise("No hay club (tenant) en el contexto de esta operacion");
        }

        @GetMapping("/t/ise-causa")
        public void iseCausa() {
            throw ErroresDeServicioFixture.iseConCausa(new java.security.GeneralSecurityException("clave interna"));
        }

        // RuntimeException / IllegalArgumentException
        @GetMapping("/t/iae-negocio")
        public void iaeNegocio() {
            throw ErroresDeServicioFixture.iae("El pago excede el saldo pendiente");
        }

        @GetMapping("/t/runtime-negocio")
        public void runtimeNegocio() {
            throw ErroresDeServicioFixture.runtime("Deportista no encontrado");
        }

        @GetMapping("/t/runtime-sin-mensaje")
        public void runtimeSinMensaje() {
            throw ErroresDeServicioFixture.runtimeSinMensaje();
        }

        @GetMapping("/t/runtime-infra")
        public void runtimeInfra() {
            throw ErroresDeInfraFixture.runtime("detalle interno de infraestructura");
        }

        // Excepciones de libreria
        @GetMapping("/t/nfe")
        public int nfe() {
            return Integer.parseInt("abc-secreto");
        }

        @GetMapping("/t/enum")
        public Object enumInvalido() {
            return Thread.State.valueOf("NO_EXISTE");
        }

        @GetMapping("/t/fecha")
        public LocalDate fecha() {
            return LocalDate.parse("2026-13-45");
        }

        @GetMapping("/t/npe")
        public int npe() {
            String s = System.getProperty("sec03.no.existe");
            return s.length();
        }

        @GetMapping("/t/classcast")
        public Integer classCast() {
            Object o = "texto";
            return (Integer) o;
        }

        @GetMapping("/t/integridad")
        public void integridad() {
            throw new DataIntegrityViolationException("could not execute statement [Duplicate entry 'ana@club.co' "
                    + "for key 'users.uk_users_email'] [insert into users (club_id, email) values (?, ?)]");
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ControladorPrueba())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---- ResponseStatusException conserva su estado y su motivo ----

    @Test
    void responseStatus409LlegaComo409ConSuMotivoSinElPrefijoDelEstado() throws Exception {
        mvc.perform(get("/t/rse-409"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("El rol Administrador no se puede eliminar.")))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void responseStatus403LlegaComo403() throws Exception {
        mvc.perform(get("/t/rse-403"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", is("No puedes modificar a un usuario de mayor jerarquia")));
    }

    @Test
    void responseStatusSinMotivoUsaLaFraseDelEstado() throws Exception {
        mvc.perform(get("/t/rse-sin-motivo"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Not Found")));
    }

    @Test
    void responseStatus5xxNoMuestraSuMotivo() throws Exception {
        mvc.perform(get("/t/rse-5xx"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is(ErrorPublico.GENERICO)))
                .andExpect(content().string(not(containsString("db-1"))));
    }

    // ---- IllegalStateException ----

    @Test
    void illegalStateDeNegocioEs409ConSuMensaje() throws Exception {
        mvc.perform(get("/t/ise-negocio"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Ya existe una sede con ese nombre")));
    }

    @Test
    void illegalStateDeInfraestructuraEs500Generico() throws Exception {
        mvc.perform(get("/t/ise-infra"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is(ErrorPublico.GENERICO)))
                .andExpect(content().string(not(containsString("tenant"))));
    }

    @Test
    void illegalStateQueEnvuelveUnaCausaEs500Generico() throws Exception {
        mvc.perform(get("/t/ise-causa"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("clave interna"))));
    }

    // ---- RuntimeException de negocio: sigue igual ----

    @Test
    void illegalArgumentDeNegocioSigueSiendo400ConSuMensaje() throws Exception {
        mvc.perform(get("/t/iae-negocio"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("El pago excede el saldo pendiente")));
    }

    @Test
    void runtimeDeNegocioSigueSiendo400ConSuMensaje() throws Exception {
        mvc.perform(get("/t/runtime-negocio"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Deportista no encontrado")));
    }

    @Test
    void runtimeSinMensajeSigueSiendo500() throws Exception {
        mvc.perform(get("/t/runtime-sin-mensaje"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is(ErrorPublico.GENERICO)));
    }

    @Test
    void runtimeDeInfraestructuraEs500YNoMuestraSuMensaje() throws Exception {
        mvc.perform(get("/t/runtime-infra"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("infraestructura"))));
    }

    // ---- libreria: ya no se filtra el texto interno ----

    @Test
    void numberFormatEs400GenericoSinElValorNiElTextoDeLaLibreria() throws Exception {
        mvc.perform(get("/t/nfe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Peticion invalida")))
                .andExpect(content().string(not(containsString("abc-secreto"))))
                .andExpect(content().string(not(containsString("For input string"))));
    }

    @Test
    void enumInvalidoEs400SinElNombreDeLaClase() throws Exception {
        mvc.perform(get("/t/enum"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Peticion invalida")))
                .andExpect(content().string(not(containsString("No enum constant"))));
    }

    @Test
    void fechaIlegibleEs400SinElTextoDeLaLibreria() throws Exception {
        mvc.perform(get("/t/fecha"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Peticion invalida")))
                .andExpect(content().string(not(containsString("2026-13-45"))));
    }

    @Test
    void nullPointerEs500SinElMensajeAyudante() throws Exception {
        mvc.perform(get("/t/npe"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is(ErrorPublico.GENERICO)))
                .andExpect(content().string(not(containsString("Cannot invoke"))));
    }

    @Test
    void classCastEs500SinNombresDeClase() throws Exception {
        mvc.perform(get("/t/classcast"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", is(ErrorPublico.GENERICO)))
                .andExpect(content().string(not(containsString("java.lang"))));
    }

    @Test
    void violacionDeIntegridadEs409SinSqlNiNombreDeConstraint() throws Exception {
        mvc.perform(get("/t/integridad"))
                .andExpect(status().isConflict())
                .andExpect(content().string(not(containsString("insert into"))))
                .andExpect(content().string(not(containsString("uk_users_email"))))
                .andExpect(content().string(not(containsString("ana@club.co"))));
    }
}
