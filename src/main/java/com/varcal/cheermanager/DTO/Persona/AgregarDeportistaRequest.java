package com.varcal.cheermanager.DTO.Persona;

import lombok.Data;

@Data
public class AgregarDeportistaRequest {
    private Integer deportistaId;
    private String observaciones; // opcional

    // Getters y setters
}
