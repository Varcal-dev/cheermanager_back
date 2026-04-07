package com.varcal.cheermanager.DTO.Persona;

import lombok.Data;
import java.time.LocalDate;

@Data
public class HistorialMedicoDTO {
    private Integer id;
    private Integer personaId;
    private String descripcion;
    private LocalDate fechaRegistro;
    private String tipoRegistro;
    private String gravedad;
    private String medicoTratante;
}