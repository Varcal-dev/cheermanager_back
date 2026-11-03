package com.varcal.cheermanager.Service.Importacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ImportacionParseoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 31);

    @Test
    void normalizaTildesMayusculasYSimbolos() {
        assertEquals("fecha_nacimiento", ImportacionParseo.normalizar("Fecha de Nacimiento".replace(" de ", " ")));
        assertEquals("acudiente_telefono", ImportacionParseo.normalizar("  Acudiente-Teléfono "));
        assertEquals("nivel_1", ImportacionParseo.normalizar("Nivel 1"));
        assertEquals("", ImportacionParseo.normalizar(null));
    }

    @Test
    void aliasDeColumnas() {
        assertEquals("numero_documento", ImportacionParseo.nombreColumna("Documento"));
        assertEquals("numero_documento", ImportacionParseo.nombreColumna("Cédula"));
        assertEquals("fecha_nacimiento", ImportacionParseo.nombreColumna("Fecha de nacimiento"));
        assertEquals("genero", ImportacionParseo.nombreColumna("Sexo"));
        assertEquals("nombre", ImportacionParseo.nombreColumna("nombre"));
    }

    @Test
    void documentoQuitaPuntosYEspacios() {
        assertEquals("1006123456", ImportacionParseo.documento(" 1.006.123.456 "));
        assertNull(ImportacionParseo.documento("  "));
        assertNull(ImportacionParseo.documento(null));
    }

    @Test
    void fechasEnLosTresFormatos() {
        assertEquals(LocalDate.of(2012, 3, 25), ImportacionParseo.fecha("2012-03-25", HOY));
        assertEquals(LocalDate.of(2012, 3, 25), ImportacionParseo.fecha("25/03/2012", HOY));
        assertEquals(LocalDate.of(2012, 3, 5), ImportacionParseo.fecha("5-3-2012", HOY));
    }

    @Test
    void fechasInvalidasFuturasOMuyAntiguas() {
        assertThrows(IllegalArgumentException.class, () -> ImportacionParseo.fecha("31/02/2012", HOY));
        assertThrows(IllegalArgumentException.class, () -> ImportacionParseo.fecha("hola", HOY));
        assertThrows(IllegalArgumentException.class, () -> ImportacionParseo.fecha("2027-01-01", HOY));
        assertThrows(IllegalArgumentException.class, () -> ImportacionParseo.fecha("1900-01-01", HOY));
    }

    @Test
    void enterosConRangoYDecimales() {
        assertEquals(152, ImportacionParseo.entero("152", "altura", 30, 260));
        assertEquals(46, ImportacionParseo.entero("45,5", "peso", 5, 300));
        assertThrows(IllegalArgumentException.class, () -> ImportacionParseo.entero("abc", "peso", 5, 300));
        assertThrows(IllegalArgumentException.class, () -> ImportacionParseo.entero("1000", "altura", 30, 260));
        assertThrows(IllegalArgumentException.class, () -> ImportacionParseo.entero("99999999999999", "peso", 5, 300));
    }

    @Test
    void correos() {
        assertTrue(ImportacionParseo.correoPlausible("ana@correo.com"));
        assertFalse(ImportacionParseo.correoPlausible("ana@correo"));
        assertFalse(ImportacionParseo.correoPlausible("ana correo.com"));
    }
}
