package com.varcal.cheermanager.DTO.Persona;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PastOrPresent;
import com.varcal.cheermanager.DTO.Comunes.Crear;
import java.time.LocalDate;
import lombok.Data;

@Data
public class DeportistaDTO {
    private Integer id;
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    private String nombre;
    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 255, message = "Los apellidos no puede superar 255 caracteres")
    private String apellidos;
    @NotBlank(groups = Crear.class, message = "El número de documento es obligatorio")
    @Size(max = 255, message = "El número de documento no puede superar 255 caracteres")
    private String numeroDocumento;
    @NotBlank(groups = Crear.class, message = "El tipo de documento es obligatorio")
    @Size(max = 255, message = "El tipo de documento no puede superar 255 caracteres")
    private String tipoDocumento;
    @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
    private String direccion;
    @Size(max = 255, message = "El teléfono no puede superar 255 caracteres")
    private String telefono;
    @PastOrPresent(message = "La fecha de nacimiento no puede ser futura")
    private LocalDate fechaNacimiento;
    @NotNull(message = "El género es obligatorio")
    private Integer generoId;
    @NotNull(message = "El estado es obligatorio")
    private Integer estadoId;
    @Min(value = 0, message = "La altura no puede ser negativa")
    @Max(value = 300, message = "La altura debe estar en centímetros (máximo 300)")
    private Integer altura;
    @Min(value = 0, message = "El peso no puede ser negativo")
    @Max(value = 500, message = "El peso debe estar en kilogramos (máximo 500)")
    private Integer peso;
    private Integer nivelActualId;
    private LocalDate fechaRegistro;
    @Size(max = 255, message = "El contacto de emergencia no puede superar 255 caracteres")
    private String contactoEmergencia;
    private Integer convenioId;
}