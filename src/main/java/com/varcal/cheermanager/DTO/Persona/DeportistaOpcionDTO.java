package com.varcal.cheermanager.DTO.Persona;

/** Lo minimo de cada deportista para llenar un selector (Objetivos, Ventas, Historial medico, Ranking). */
public record DeportistaOpcionDTO(
        Integer deportistaId,
        Integer personaId,
        String nombre,
        String apellidos) {
}
