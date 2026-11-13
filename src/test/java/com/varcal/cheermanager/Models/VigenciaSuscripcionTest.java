package com.varcal.cheermanager.Models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Clubes.Club;
import com.varcal.cheermanager.Models.Clubes.EstadoSuscripcion;
import com.varcal.cheermanager.Models.Clubes.VigenciaSuscripcion;

/** Reglas de vigencia de la suscripcion (funcion pura: no necesita base de datos). */
class VigenciaSuscripcionTest {

    private static final LocalDate HOY = LocalDate.of(2027, 2, 15);

    private static Club club(int id, EstadoSuscripcion estado, LocalDate hasta, int gracia) {
        Club c = new Club();
        c.setId(id);
        c.setEstadoSuscripcion(estado);
        c.setVigenteHasta(hasta);
        c.setDiasGracia(gracia);
        return c;
    }

    @Test
    void plataformaNuncaVence() {
        assertEquals(VigenciaSuscripcion.ILIMITADA,
                VigenciaSuscripcion.de(club(Club.PLATAFORMA_ID, EstadoSuscripcion.CANCELADA, HOY.minusYears(1), 0), HOY));
    }

    @Test
    void sinFechaNoVence() {
        assertEquals(VigenciaSuscripcion.ILIMITADA, VigenciaSuscripcion.de(club(2, EstadoSuscripcion.ACTIVA, null, 5), HOY));
    }

    @Test
    void elUltimoDiaVigenteEsInclusivo() {
        assertEquals(VigenciaSuscripcion.VIGENTE, VigenciaSuscripcion.de(club(2, EstadoSuscripcion.ACTIVA, HOY, 5), HOY));
        assertEquals(VigenciaSuscripcion.VIGENTE, VigenciaSuscripcion.de(club(2, EstadoSuscripcion.PRUEBA, HOY.plusDays(10), 5), HOY));
    }

    @Test
    void activaVencidaTieneDiasDeGracia() {
        assertEquals(VigenciaSuscripcion.EN_GRACIA,
                VigenciaSuscripcion.de(club(2, EstadoSuscripcion.ACTIVA, HOY.minusDays(1), 5), HOY));
        assertEquals(VigenciaSuscripcion.EN_GRACIA,
                VigenciaSuscripcion.de(club(2, EstadoSuscripcion.ACTIVA, HOY.minusDays(5), 5), HOY));
        assertEquals(VigenciaSuscripcion.SOLO_LECTURA,
                VigenciaSuscripcion.de(club(2, EstadoSuscripcion.ACTIVA, HOY.minusDays(6), 5), HOY));
    }

    @Test
    void pruebaVencidaPasaDirectoASoloLectura() {
        assertEquals(VigenciaSuscripcion.SOLO_LECTURA,
                VigenciaSuscripcion.de(club(2, EstadoSuscripcion.PRUEBA, HOY.minusDays(1), 5), HOY));
    }

    @Test
    void canceladaEsSoloLecturaAunConFechaFutura() {
        assertEquals(VigenciaSuscripcion.SOLO_LECTURA,
                VigenciaSuscripcion.de(club(2, EstadoSuscripcion.CANCELADA, HOY.plusMonths(3), 5), HOY));
    }

    @Test
    void clubNuevoNaceEnPruebaDe30Dias() {
        Club nuevo = new Club();
        nuevo.setId(7);
        assertEquals(EstadoSuscripcion.PRUEBA, nuevo.getEstadoSuscripcion());
        assertEquals(VigenciaSuscripcion.VIGENTE, VigenciaSuscripcion.de(nuevo, com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy()));
    }
}
