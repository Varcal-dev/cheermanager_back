package com.varcal.cheermanager.DTO.Persona;

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
    private String descripcion;
    private LocalDate fechaRegistro;
    private String tipoRegistro;
    private String gravedad;
    private String medicoTratante;
}