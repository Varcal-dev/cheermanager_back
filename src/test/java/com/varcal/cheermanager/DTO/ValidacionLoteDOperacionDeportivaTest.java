package com.varcal.cheermanager.DTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Controller.Eventos.RosterController;
import com.varcal.cheermanager.Controller.Horario_Asistencia.AsistenciaController.EscaneoRequest;
import com.varcal.cheermanager.DTO.Evaluaciones.EvaluacionDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.RegistroEvaluacionMasivoDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.Rutina.EvaluacionRutinaDTO;
import com.varcal.cheermanager.DTO.Horario_Asistencia.AsistenciaDTO;
import com.varcal.cheermanager.DTO.Horario_Asistencia.RegistroAsistenciaMasivoDTO;
import com.varcal.cheermanager.DTO.Persona.AsignacionEntrenadorUpdateDTO;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * SEC-02 fase 5D (operacion deportiva): ids y fechas obligatorios en evaluaciones, asistencias, roster y rutina.
 * Los lotes (masivo) no validan cada fila a proposito: un registro malo sigue reportandose por deportista.
 */
class ValidacionLoteDOperacionDeportivaTest {

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

    @Test
    void evaluacion_individual_y_masiva_exigen_deportista_y_criterio() {
        assertEquals(Set.of("deportistaId", "criterioId"), campos(new EvaluacionDTO()));
        assertEquals(Set.of("deportistaId"), campos(new RegistroEvaluacionMasivoDTO()));
        RegistroEvaluacionMasivoDTO m = new RegistroEvaluacionMasivoDTO();
        m.setDeportistaId(1);
        m.setRegistros(List.of(new RegistroEvaluacionMasivoDTO.RegistroIndividual())); // la fila mala no tumba el lote
        assertTrue(campos(m).isEmpty());
    }

    @Test
    void asistencia_individual_exige_deportista_horario_fecha_y_estado() {
        assertEquals(Set.of("deportistaId", "horarioId", "fecha", "estadoAsistenciaId"), campos(new AsistenciaDTO()));
        AsistenciaDTO d = new AsistenciaDTO();
        d.setDeportistaId(1);
        d.setHorarioId(2);
        d.setFecha(LocalDate.of(2026, 11, 27));
        d.setEstadoAsistenciaId(1);
        assertTrue(campos(d).isEmpty());
    }

    @Test
    void asistencia_masiva_exige_horario_fecha_y_registros_pero_no_valida_cada_fila() {
        assertEquals(Set.of("horarioId", "fecha", "registros"), campos(new RegistroAsistenciaMasivoDTO()));
        RegistroAsistenciaMasivoDTO m = new RegistroAsistenciaMasivoDTO();
        m.setHorarioId(2);
        m.setFecha(LocalDate.of(2026, 11, 27));
        m.setRegistros(List.of(new RegistroAsistenciaMasivoDTO.RegistroIndividual()));
        assertTrue(campos(m).isEmpty());
    }

    @Test
    void escaneo_y_roster_exigen_sus_identificadores() {
        assertEquals(Set.of("codigo", "horarioId"), campos(new EscaneoRequest(" ", null, null)));
        assertTrue(campos(new EscaneoRequest("abc", 3, null)).isEmpty());
        assertEquals(Set.of("deportistaId"), campos(new RosterController.Agregar(null, null, null)));
    }

    @Test
    void asignacion_de_entrenador_acepta_solo_fecha_iso_o_vacia() {
        AsignacionEntrenadorUpdateDTO d = new AsignacionEntrenadorUpdateDTO();
        assertTrue(campos(d).isEmpty()); // solo rol (lo que envia el front) o nada
        d.setFechaFin("");
        assertTrue(campos(d).isEmpty());
        d.setFechaFin("2026-11-27");
        assertTrue(campos(d).isEmpty());
        d.setFechaFin("27/11/2026");
        assertEquals(Set.of("fechaFin"), campos(d));
        d.setFechaFin(null);
        d.setRol(x(256));
        assertEquals(Set.of("rol"), campos(d));
    }

    @Test
    void evaluacion_de_rutina_exige_grupo_nivel_fecha_atletas_y_registros() {
        assertEquals(Set.of("grupoId", "nivelId", "fecha", "cantidadAtletas", "registros"), campos(new EvaluacionRutinaDTO()));
        EvaluacionRutinaDTO d = new EvaluacionRutinaDTO();
        d.setGrupoId(1);
        d.setNivelId(2);
        d.setFecha(LocalDate.of(2026, 11, 27));
        d.setCantidadAtletas(0);
        d.setRegistros(List.of());
        d.setEvento(x(201));
        d.setObservaciones(x(1001));
        assertEquals(Set.of("cantidadAtletas", "evento", "observaciones"), campos(d));
        d.setCantidadAtletas(20);
        d.setEvento(null);
        d.setObservaciones(null);
        assertTrue(campos(d).isEmpty());
    }
}
