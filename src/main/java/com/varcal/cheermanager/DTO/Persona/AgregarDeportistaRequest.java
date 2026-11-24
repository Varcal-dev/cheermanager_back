package com.varcal.cheermanager.DTO.Persona;

import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class AgregarDeportistaRequest {
    @NotNull(message = "El deportista es obligatorio")
    private Integer deportistaId;
    private String observaciones; // opcional

    // Getters y setters
}