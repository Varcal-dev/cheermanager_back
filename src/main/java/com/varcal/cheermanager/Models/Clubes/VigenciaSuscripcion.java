package com.varcal.cheermanager.Models.Clubes;

import java.time.LocalDate;

/**
 * Lo que un club puede hacer hoy segun su suscripcion. Funcion pura (sin base de datos) para poder
 * probarla y usarla desde el filtro de seguridad.
 */
public enum VigenciaSuscripcion {
    /** Sin vencimiento (club Plataforma, plan fundador o vigente_hasta vacio). */
    ILIMITADA,
    /** Dentro del periodo pagado o de la prueba. */
    VIGENTE,
    /** Vencida pero dentro de los dias de gracia: todo funciona y se muestra un aviso. */
    EN_GRACIA,
    /** Vencida o cancelada: solo se puede consultar (GET); no se puede crear ni modificar nada. */
    SOLO_LECTURA;

    public static VigenciaSuscripcion de(Club club, LocalDate hoy) {
        if (club.getId() != null && club.getId() == Club.PLATAFORMA_ID) {
            return ILIMITADA;
        }
        EstadoSuscripcion estado = club.getEstadoSuscripcion() == null ? EstadoSuscripcion.ACTIVA : club.getEstadoSuscripcion();
        if (estado == EstadoSuscripcion.CANCELADA) {
            return SOLO_LECTURA;
        }
        LocalDate hasta = club.getVigenteHasta();
        if (hasta == null) {
            return ILIMITADA;
        }
        if (!hoy.isAfter(hasta)) {
            return VIGENTE;
        }
        int gracia = club.getDiasGracia() == null ? 0 : Math.max(0, club.getDiasGracia());
        if (estado == EstadoSuscripcion.ACTIVA && !hoy.isAfter(hasta.plusDays(gracia))) {
            return EN_GRACIA;
        }
        return SOLO_LECTURA;
    }

    public boolean soloLectura() {
        return this == SOLO_LECTURA;
    }
}
