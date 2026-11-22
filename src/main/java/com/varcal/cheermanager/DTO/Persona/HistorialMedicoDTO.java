package com.varcal.cheermanager.DTO.Persona;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class HistorialMedicoDTO {
    private Integer id;
    private Integer personaId;
    // Solo se completan en el listado global (listarTodos) — la vista de
    // club completo necesita mostrar de quién es cada registro sin que el
    // front tenga que resolverlo aparte.
    private String personaNombre;
    private String personaApellidos;
    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    private String descripcion;
    @NotNull(message = "La fecha de registro es obligatoria")
    private LocalDate fechaRegistro;
    @Size(max = 255, message = "El tipo de registro no puede superar 255 caracteres")
    private String tipoRegistro;
    @Size(max = 255, message = "La gravedad no puede superar 255 caracteres")
    private String gravedad;
    @Size(max = 255, message = "El médico tratante no puede superar 255 caracteres")
    private String medicoTratante;
}