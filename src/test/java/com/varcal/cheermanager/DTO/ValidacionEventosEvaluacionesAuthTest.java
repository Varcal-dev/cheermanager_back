package com.varcal.cheermanager.DTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Controller.Auth.AuthController.ChangePasswordRequest;
import com.varcal.cheermanager.Controller.Auth.AuthController.ForgotPasswordRequest;
import com.varcal.cheermanager.Controller.Auth.AuthController.LoginRequest;
import com.varcal.cheermanager.Controller.Auth.AuthController.ResetPasswordRequest;
import com.varcal.cheermanager.DTO.Evaluaciones.CriterioEvaluacionDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.ObjetivoDTO;
import com.varcal.cheermanager.DTO.Eventos.EventoDTO;
import com.varcal.cheermanager.DTO.Eventos.GrupoEventoDTO;
import com.varcal.cheermanager.DTO.Eventos.PremioEventoDTO;
import com.varcal.cheermanager.DTO.Eventos.ResultadoCompetenciaDTO;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/** SEC-02 fase 3: login y claves, eventos y competencias, criterios y objetivos. */
class ValidacionEventosEvaluacionesAuthTest {

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

    // ---------- Auth ----------

    @Test
    void login_exige_correo_y_clave() {
        LoginRequest r = new LoginRequest();
        assertEquals(Set.of("email", "password"), campos(r));
        r.setEmail("  ");
        r.setPassword("");
        assertEquals(Set.of("email", "password"), campos(r));
        r.setEmail("ana@club.com");
        r.setPassword("cualquier-clave");
        assertTrue(campos(r).isEmpty());
    }

    @Test
    void login_limita_el_largo() {
        LoginRequest r = new LoginRequest();
        r.setEmail("a".repeat(256));
        r.setPassword("p".repeat(256));
        assertEquals(Set.of("email", "password"), campos(r));
    }

    @Test
    void forgot_reset_y_cambio_de_clave_solo_limitan_largo() {
        // Lo obligatorio y el minimo de 8 siguen en el controlador/servicio (mensajes propios).
        assertTrue(campos(new ForgotPasswordRequest()).isEmpty());
        assertTrue(campos(new ResetPasswordRequest()).isEmpty());
        assertTrue(campos(new ChangePasswordRequest()).isEmpty());
        ResetPasswordRequest r = new ResetPasswordRequest();
        r.setToken("t".repeat(256));
        r.setNewPassword("n".repeat(73));
        assertEquals(Set.of("token", "newPassword"), campos(r));
        ChangePasswordRequest c = new ChangePasswordRequest();
        c.setCurrentPassword("c".repeat(256));
        c.setNewPassword("n".repeat(73));
        assertEquals(Set.of("currentPassword", "newPassword"), campos(c));
    }

    // ---------- Evento ----------

    @Test
    void evento_exige_nombre_fecha_y_tipo_y_acepta_fechas_pasadas() {
        assertEquals(Set.of("nombre", "fecha", "tipoEventoId"), campos(new EventoDTO()));
        EventoDTO e = new EventoDTO();
        e.setNombre("Nacional");
        e.setFecha(LocalDate.of(2020, 1, 1)); // historial de eventos pasados
        e.setTipoEventoId(1);
        assertTrue(campos(e).isEmpty());
        e.setUbicacion("u".repeat(256));
        assertEquals(Set.of("ubicacion"), campos(e));
    }

    // ---------- Resultado de competencia ----------

    private static ResultadoCompetenciaDTO resultado() {
        ResultadoCompetenciaDTO r = new ResultadoCompetenciaDTO();
        r.setEventoId(1);
        r.setGrupoEntrenamientoId(2);
        r.setPosicion(1);
        r.setPuntaje(new BigDecimal("87.50"));
        return r;
    }

    @Test
    void resultado_valido_y_limites_del_porcentaje() {
        assertTrue(campos(resultado()).isEmpty());
        ResultadoCompetenciaDTO r = resultado();
        r.setPuntaje(BigDecimal.ZERO);
        assertTrue(campos(r).isEmpty());
        r.setPuntaje(new BigDecimal("100"));
        assertTrue(campos(r).isEmpty());
        r.setPuntaje(new BigDecimal("100.01"));
        assertEquals(Set.of("puntaje"), campos(r));
        r.setPuntaje(new BigDecimal("-1"));
        assertEquals(Set.of("puntaje"), campos(r));
        r.setPuntaje(new BigDecimal("50.123")); // mas de 2 decimales
        assertEquals(Set.of("puntaje"), campos(r));
        r.setPuntaje(null); // la columna admite NULL
        assertTrue(campos(r).isEmpty());
    }

    @Test
    void resultado_exige_evento_y_grupo_posicion_minima_1_y_observaciones_cortas() {
        assertEquals(Set.of("eventoId", "grupoEntrenamientoId"), campos(new ResultadoCompetenciaDTO()));
        ResultadoCompetenciaDTO r = resultado();
        r.setPosicion(0);
        r.setObservaciones("o".repeat(256));
        assertEquals(Set.of("posicion", "observaciones"), campos(r));
    }

    // ---------- Premio y grupo-evento ----------

    @Test
    void premio_y_grupo_evento() {
        assertEquals(Set.of("eventoId"), campos(new PremioEventoDTO()));
        PremioEventoDTO p = new PremioEventoDTO();
        p.setEventoId(1);
        p.setDescripcion("d".repeat(256));
        p.setPremio("p".repeat(256));
        assertEquals(Set.of("descripcion", "premio"), campos(p));
        assertEquals(Set.of("grupoEntrenamientoId", "eventoId"), campos(new GrupoEventoDTO()));
    }

    // ---------- Criterios y objetivos ----------

    @Test
    void criterio_exige_nombre_y_categoria() {
        assertEquals(Set.of("nombre", "categoriaId"), campos(new CriterioEvaluacionDTO()));
        CriterioEvaluacionDTO c = new CriterioEvaluacionDTO();
        c.setNombre("   ");
        c.setCategoriaId(1);
        assertEquals(Set.of("nombre"), campos(c));
        c.setNombre("Flexibilidad");
        assertTrue(campos(c).isEmpty());
    }

    @Test
    void objetivo_exige_deportista_nombre_y_estado_pero_no_la_fecha() {
        assertEquals(Set.of("deportistaId", "nombre", "estadoObjetivoId"), campos(new ObjetivoDTO()));
        ObjetivoDTO o = new ObjetivoDTO();
        o.setDeportistaId(1);
        o.setNombre("Hacer split");
        o.setEstadoObjetivoId(1);
        assertTrue(campos(o).isEmpty()); // fechaCreacion: el servicio pone hoy
        o.setDescripcion("d".repeat(256));
        assertEquals(Set.of("descripcion"), campos(o));
    }
}
