package com.varcal.cheermanager.Service.Importacion;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

class CsvUtilTest {

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void leeConPuntoYComaYQuitaElBOM() {
        List<String[]> a = CsvUtil.leer(utf8("﻿nombre;apellidos\r\nAna;Gómez\r\n"));
        assertEquals(2, a.size());
        assertArrayEquals(new String[] { "nombre", "apellidos" }, a.get(0));
        assertArrayEquals(new String[] { "Ana", "Gómez" }, a.get(1));

        List<String[]> b = CsvUtil.leer(utf8("nombre,apellidos\nAna,Gómez\n"));
        assertArrayEquals(new String[] { "Ana", "Gómez" }, b.get(1));

        List<String[]> c = CsvUtil.leer(utf8("nombre\tapellidos\nAna\tGómez"));
        assertArrayEquals(new String[] { "Ana", "Gómez" }, c.get(1));
    }

    @Test
    void comillasConSeparadorSaltoDeLineaYComillaEscapada() {
        List<String[]> f = CsvUtil.leer(utf8("a;b;c\n\"x;y\";\"linea1\nlinea2\";\"dijo \"\"hola\"\"\"\n"));
        assertEquals(2, f.size());
        assertArrayEquals(new String[] { "x;y", "linea1\nlinea2", "dijo \"hola\"" }, f.get(1));
    }

    @Test
    void ignoraLineasVaciasYAceptaUltimaFilaSinSaltoFinal() {
        List<String[]> f = CsvUtil.leer(utf8("a;b\n\n;;\nx;y"));
        assertEquals(2, f.size());
        assertArrayEquals(new String[] { "x", "y" }, f.get(1));
    }

    @Test
    void siNoEsUtf8UsaWindows1252() {
        byte[] datos = "nombre;apellidos\nJosé;Peña\n".getBytes(Charset.forName("windows-1252"));
        List<String[]> f = CsvUtil.leer(datos);
        assertArrayEquals(new String[] { "José", "Peña" }, f.get(1));
    }

    @Test
    void escribeConBOMSeparadorYComillas() {
        byte[] b = CsvUtil.escribir(List.of(List.of("a", "x;y", "dijo \"hola\""), List.of("1", "", "z")));
        String s = new String(b, StandardCharsets.UTF_8);
        assertEquals("﻿a;\"x;y\";\"dijo \"\"hola\"\"\"\r\n1;;z\r\n", s);
    }

    @Test
    void escribirYLeerSeRecuperan() {
        List<List<String>> filas = List.of(List.of("nombre", "nota"), List.of("Ñandú; \"el\" rápido", "a\nb"));
        List<String[]> leidas = CsvUtil.leer(CsvUtil.escribir(filas));
        assertEquals(2, leidas.size());
        assertArrayEquals(new String[] { "Ñandú; \"el\" rápido", "a\nb" }, leidas.get(1));
    }

    @Test
    void protegeContraFormulas() {
        assertEquals("'=SUMA(A1)", CsvUtil.celda("=SUMA(A1)"));
        assertEquals("'@cmd", CsvUtil.celda("@cmd"));
        assertEquals("'+cmd|x", CsvUtil.celda("+cmd|x"));
        assertEquals("'-cmd", CsvUtil.celda("-cmd"));
        // telefonos y numeros normales no se tocan
        assertEquals("+573001234567", CsvUtil.celda("+573001234567"));
        assertEquals("-5", CsvUtil.celda("-5"));
        assertEquals("", CsvUtil.celda(null));
        assertTrue(CsvUtil.celda("normal").equals("normal"));
    }
}
