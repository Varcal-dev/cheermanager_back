package com.varcal.cheermanager.DTO.Notificaciones;

/** Que canales estan listos en este servidor: sistema siempre; correo con SMTP; whatsappApi con credenciales de Meta. */
public record CanalesDisponiblesDTO(boolean sistema, boolean correo, boolean whatsappApi) {
}
