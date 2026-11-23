package com.varcal.cheermanager.DTO.Persona;

/** Resultado ligero de la busqueda de deportistas por nombre (paleta de comandos). */
public record DeportistaBusquedaDTO(
        Integer deportistaId,
        Integer personaId,
        String nombre,
        String apellidos,
        String nivelNombre) {
}
