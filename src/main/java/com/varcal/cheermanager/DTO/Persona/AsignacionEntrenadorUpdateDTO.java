package com.varcal.cheermanager.DTO.Persona;

import jakarta.validation.constraints.*;

import lombok.Data;

@Data
public class AsignacionEntrenadorUpdateDTO {
    @Size(max = 255, message = "El rol admite maximo 255 caracteres")
    private String rol;
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "La fecha de fin debe tener el formato aaaa-mm-dd")
    private String fechaFin; // formato ISO (yyyy-MM-dd), puede venir null para reabrir la asignación
}