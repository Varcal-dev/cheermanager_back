package com.varcal.cheermanager.Models.Clubes;

/** Estado comercial de la suscripcion de un club. La vigencia real se calcula con {@link VigenciaSuscripcion}. */
public enum EstadoSuscripcion {
    /** Prueba gratis: vence en vigente_hasta y, vencida, pasa directo a solo lectura (sin gracia). */
    PRUEBA,
    /** Suscripcion pagada: al vencer tiene dias de gracia antes de pasar a solo lectura. */
    ACTIVA,
    /** Cancelada: el club solo puede consultar sus datos. */
    CANCELADA
}
