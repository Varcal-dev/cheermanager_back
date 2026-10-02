package com.varcal.cheermanager.DTO.Persona;

import lombok.Data;

@Data
public class AsignacionEntrenadorUpdateDTO {
    private String rol;
    private String fechaFin; // formato ISO (yyyy-MM-dd), puede venir null para reabrir la asignación
}