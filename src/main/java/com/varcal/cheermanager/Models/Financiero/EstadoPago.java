package com.varcal.cheermanager.Models.Financiero;

public enum EstadoPago {
    Pagado,
    Pendiente,
    Vencido,
    /** Solo se llega aqui por PATCH /api/pagos/{id}/anular (con motivo). No suma al saldo. */
    Anulado

}