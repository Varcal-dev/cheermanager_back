package com.varcal.cheermanager.Service.Evaluaciones.Rutina;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Service.Evaluaciones.Rutina.RubricaValidacion.RangoExistente;

class RubricaValidacionTest {

    private static String error(Runnable r) {
        try {
            r.run();
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
        return null;
    }

    private static final List<RangoExistente> EXISTENTES = List.of(new RangoExistente(1, 5, 11), new RangoExistente(2, 12, 17));

    @Test
    void tablaCantidadValida() {
        assertEquals(null, error(() -> RubricaValidacion.tablaCantidad("Construcciones", "grupos", 18, 22, 3, 4, 5, EXISTENTES)));
        assertEquals(null, error(() -> RubricaValidacion.tablaCantidad("Saltos_Gimnasia", "atletas", 5, 11, 5, 6, 10, List.of())));
    }

    @Test
    void tablaCantidadInvalida() {
        assertEquals(true, error(() -> RubricaValidacion.tablaCantidad("Otra", "grupos", 1, 2, 1, 1, 1, null)).contains("tabla"));
        assertEquals(true, error(() -> RubricaValidacion.tablaCantidad("Construcciones", "atletas", 1, 2, 1, 1, 1, null)).contains("unidad"));
        assertEquals(true, error(() -> RubricaValidacion.tablaCantidad("Construcciones", "grupos", 10, 5, 1, 2, 3, null)).contains("rango"));
        assertEquals(true, error(() -> RubricaValidacion.tablaCantidad("Construcciones", "grupos", 0, 5, 1, 2, 3, null)).contains("rango"));
        assertEquals(true, error(() -> RubricaValidacion.tablaCantidad("Construcciones", "grupos", 1, 5, 3, 2, 3, null)).contains("<="));
        assertEquals(true, error(() -> RubricaValidacion.tablaCantidad("Construcciones", "grupos", 1, 5, 1, 4, 3, null)).contains("<="));
        assertEquals(true, error(() -> RubricaValidacion.tablaCantidad("Construcciones", "grupos", 1, 5, null, 2, 3, null)).contains("obligatorios"));
    }

    @Test
    void tablaCantidadNoSePisaConOtrosRangos() {
        assertEquals(true, error(() -> RubricaValidacion.tablaCantidad("Construcciones", "grupos", 11, 14, 1, 2, 3, EXISTENTES)).contains("se cruza"));
        assertEquals(true, error(() -> RubricaValidacion.tablaCantidad("Construcciones", "grupos", 1, 30, 1, 2, 3, EXISTENTES)).contains("se cruza"));
        assertEquals(null, error(() -> RubricaValidacion.tablaCantidad("Construcciones", "grupos", 1, 4, 1, 2, 3, EXISTENTES)));
    }

    @Test
    void topeSeccion() {
        BigDecimal v = new BigDecimal("4.50");
        assertEquals(null, error(() -> RubricaValidacion.topeSeccion(5, v, 4, "GRAN_PARTE", "texto", List.of(1, 2))));
        assertEquals(null, error(() -> RubricaValidacion.topeSeccion(1, BigDecimal.ZERO, null, null, null, List.of())));
        assertEquals(null, error(() -> RubricaValidacion.topeSeccion(1, v, null, "  ", null, null)));
        assertEquals(true, error(() -> RubricaValidacion.topeSeccion(0, v, 4, null, null, null)).contains("orden"));
        assertEquals(true, error(() -> RubricaValidacion.topeSeccion(1, new BigDecimal("-1"), null, null, null, null)).contains("valor"));
        assertEquals(true, error(() -> RubricaValidacion.topeSeccion(1, new BigDecimal("10.01"), null, null, null, null)).contains("valor"));
        assertEquals(true, error(() -> RubricaValidacion.topeSeccion(1, v, 0, null, null, null)).contains("habilidades"));
        assertEquals(true, error(() -> RubricaValidacion.topeSeccion(1, v, 2, "TODOS", null, null)).contains("MAYORIA"));
        assertEquals(true, error(() -> RubricaValidacion.topeSeccion(2, v, 2, null, null, List.of(1, 2))).contains("Ya existe"));
        assertEquals(true, error(() -> RubricaValidacion.topeSeccion(2, v, 2, null, "x".repeat(501), null)).contains("500"));
    }

    @Test
    void nivelesYSubCriterios() {
        assertEquals(null, error(() -> RubricaValidacion.nombreNivel(" N5 ", "Elite")));
        assertEquals("N5", RubricaValidacion.nombreNivel(" N5 ", "Elite"));
        assertEquals(true, error(() -> RubricaValidacion.nombreNivel(" ", "Elite")).contains("nombre"));
        assertEquals(true, error(() -> RubricaValidacion.nombreNivel("N5", null)).contains("odalidad"));
        assertEquals(true, error(() -> RubricaValidacion.nombreNivel("x".repeat(51), "Elite")).contains("50"));
        assertEquals(null, error(() -> RubricaValidacion.subCriterio("Nuevo", "Gimnasia", "X")));
        assertEquals(true, error(() -> RubricaValidacion.subCriterio("Nuevo", "Otra", "X")).contains("seccion"));
        assertEquals(true, error(() -> RubricaValidacion.subCriterio("Nuevo", "General", null)).contains("tipo"));
        assertEquals("grupos", RubricaValidacion.unidadEsperada("Construcciones"));
        assertEquals("atletas", RubricaValidacion.unidadEsperada("Saltos_Gimnasia"));
    }
}
