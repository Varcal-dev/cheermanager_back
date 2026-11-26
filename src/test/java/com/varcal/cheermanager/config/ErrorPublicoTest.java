package com.varcal.cheermanager.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.varcal.cheermanager.Service.ErroresDeServicioFixture;
import com.varcal.cheermanager.Tenant.ErroresDeInfraFixture;

/**
 * SEC-03 fase 1: ErrorPublico muestra solo los mensajes que escribimos nosotros y oculta los de libreria,
 * base de datos e infraestructura.
 */
class ErrorPublicoTest {

    private static final String SQL = "could not execute statement [Duplicate entry 'ana@club.co' for key "
            + "'users.uk_users_email'] [insert into users (club_id, email, password) values (?, ?, ?)]";

    // ---- se muestran ----

    @Test
    void illegalArgumentDeNegocioEsPublica() {
        var e = ErroresDeServicioFixture.iae("El pago excede el saldo pendiente");
        assertTrue(ErrorPublico.esPublico(e));
        assertEquals("El pago excede el saldo pendiente", ErrorPublico.de(e));
    }

    @Test
    void illegalStateDeNegocioEsPublica() {
        var e = ErroresDeServicioFixture.ise("Ya existe una sede con ese nombre");
        assertTrue(ErrorPublico.esPublico(e));
        assertEquals("Ya existe una sede con ese nombre", ErrorPublico.de(e));
    }

    @Test
    void runtimeExactaDeNegocioEsPublica() {
        var e = ErroresDeServicioFixture.runtime("Deportista no encontrado");
        assertTrue(ErrorPublico.esPublico(e));
    }

    @Test
    void runtimeConCausaPeroMensajePropioEsPublica() {
        var e = ErroresDeServicioFixture.runtimeConCausa("No se pudo enviar el correo", new java.io.IOException("smtp"));
        assertTrue(ErrorPublico.esPublico(e));
        assertEquals("No se pudo enviar el correo", ErrorPublico.de(e));
    }

    @Test
    void excepcionCreadaDentroDeUnaLambdaDeNegocioEsPublica() {
        assertTrue(ErrorPublico.esPublico(ErroresDeServicioFixture.iaeDesdeLambda("Grupo no encontrado")));
    }

    @Test
    void claseDeExcepcionPropiaEsPublicaVengaDeDondeVenga() {
        assertTrue(ErrorPublico.esPublico(new ErroresDeServicioFixture.ErrorPropio("Asignacion invalida")));
        assertTrue(ErrorPublico.esPublico(new ErroresDeInfraFixture.ErrorPropioDeInfra("Demasiados intentos")));
    }

    // ---- se ocultan: libreria ----

    @Test
    void numberFormatDeIntegerParseIntSeOculta() {
        NumberFormatException e = assertThrowsParse("abc-secreto");
        assertFalse(ErrorPublico.esPublico(e));
        assertEquals(ErrorPublico.GENERICO, ErrorPublico.de(e));
    }

    @Test
    void enumValueOfSeOcultaYNoFiltraElNombreDeLaClase() {
        IllegalArgumentException e = null;
        try {
            Thread.State.valueOf("NO_EXISTE");
        } catch (IllegalArgumentException ex) {
            e = ex;
        }
        assertNotNull(e, "Enum.valueOf debia fallar");
        assertFalse(ErrorPublico.esPublico(e));
        assertFalse(ErrorPublico.de(e).contains("No enum constant"));
    }

    @Test
    void illegalArgumentDeSpringAssertSeOculta() {
        IllegalArgumentException e = null;
        try {
            org.springframework.util.Assert.notNull(null, "interno: el repositorio no puede ser nulo");
        } catch (IllegalArgumentException ex) {
            e = ex;
        }
        assertNotNull(e, "Assert.notNull debia fallar");
        assertFalse(ErrorPublico.esPublico(e));
    }

    @Test
    void subclaseDeIllegalArgumentNoSeTrataComoMensajeDeNegocioAunqueSeCreeEnUnService() {
        assertFalse(ErrorPublico.esPublico(ErroresDeServicioFixture.nfe("For input string: \"x\"")));
    }

    @Test
    void nullPointerConMensajeAyudanteSeOculta() {
        NullPointerException e = null;
        try {
            String s = System.getProperty("sec03.no.existe");
            s.length();
        } catch (NullPointerException ex) {
            e = ex;
        }
        assertNotNull(e, "s.length() debia lanzar NullPointerException");
        assertFalse(ErrorPublico.esPublico(e));
        assertEquals(ErrorPublico.GENERICO, ErrorPublico.de(e));
    }

    @Test
    void errorDeBaseDeDatosConSqlSeOculta() {
        var e = new DataIntegrityViolationException(SQL);
        assertFalse(ErrorPublico.esPublico(e));
        String visible = ErrorPublico.de(e);
        assertFalse(visible.contains("insert into"));
        assertFalse(visible.contains("uk_users_email"));
    }

    // ---- se ocultan: infraestructura y formas dudosas ----

    @Test
    void excepcionesCreadasEnInfraestructuraSeOcultan() {
        assertFalse(ErrorPublico.esPublico(ErroresDeInfraFixture.ise("No hay club (tenant) en el contexto de esta operacion")));
        assertFalse(ErrorPublico.esPublico(ErroresDeInfraFixture.iae("detalle interno")));
        assertFalse(ErrorPublico.esPublico(ErroresDeInfraFixture.runtime("detalle interno")));
    }

    @Test
    void illegalStateEnvolviendoUnaCausaSeOcultaPorqueSuMensajeEsLaCausa() {
        var causa = new java.security.GeneralSecurityException("clave interna invalida");
        var e = ErroresDeServicioFixture.iseConCausa(causa);
        assertEquals(causa.toString(), e.getMessage());
        assertFalse(ErrorPublico.esPublico(e));
    }

    @Test
    void mensajeNuloVacioOEnBlancoSeOcultaYNuncaDevuelveNull() {
        assertFalse(ErrorPublico.esPublico(ErroresDeServicioFixture.runtimeSinMensaje()));
        assertFalse(ErrorPublico.esPublico(ErroresDeServicioFixture.runtime("")));
        assertFalse(ErrorPublico.esPublico(ErroresDeServicioFixture.runtime("   ")));
        assertEquals(ErrorPublico.GENERICO, ErrorPublico.de(ErroresDeServicioFixture.runtimeSinMensaje()));
    }

    @Test
    void nullSeOculta() {
        assertFalse(ErrorPublico.esPublico(null));
        assertEquals(ErrorPublico.GENERICO, ErrorPublico.de(null));
    }

    private static NumberFormatException assertThrowsParse(String texto) {
        try {
            Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            return e;
        }
        throw new AssertionError("Integer.parseInt debia fallar");
    }
}
