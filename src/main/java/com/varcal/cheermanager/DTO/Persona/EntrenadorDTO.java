package com.varcal.cheermanager.DTO.Persona;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PastOrPresent;
import com.varcal.cheermanager.DTO.Comunes.Crear;
import java.time.LocalDate;

import lombok.Data;

@Data
public class EntrenadorDTO {
    private Integer rolIdE;
    @NotBlank(groups = Crear.class, message = "El nombre es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    private String nombre;
    @NotBlank(groups = Crear.class, message = "Los apellidos son obligatorios")
    @Size(max = 255, message = "Los apellidos no puede superar 255 caracteres")
    private String apellidos;
    @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
    private String direccion;
    @Size(max = 255, message = "El teléfono no puede superar 255 caracteres")
    private String telefono;
    @PastOrPresent(message = "La fecha de nacimiento no puede ser futura")
    private LocalDate fechaNacimiento;
    @NotNull(groups = Crear.class, message = "El género es obligatorio")
    private Integer generoId;
    @Size(max = 255, message = "La especialización no puede superar 255 caracteres")
    private String especializacion;
    @NotNull(groups = Crear.class, message = "La fecha de contratación es obligatoria")
    private LocalDate fechaContratacion;
    @NotNull(groups = Crear.class, message = "El estado es obligatorio")
    private Integer estadoId;

    
}