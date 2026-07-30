// src/main/java/com/varcal/cheermanager/DTO/Org_dep/GruposElegiblesResponseDTO.java
package com.varcal.cheermanager.DTO.Org_dep;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Respuesta de GET /api/deportistas/{id}/grupos-elegibles.
// Usada por el paso 3 del asistente de matrícula para sugerir grupos sin
// que el front tenga que traer /api/grupos/con-deportistas completo y
// filtrar a ciegas por nivel (que además no valida año de nacimiento ni cupo).
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GruposElegiblesResponseDTO {
    private Integer deportistaId;
    private String nombreCompleto;
    private List<GrupoElegibleDTO> elegibles;
    private List<GrupoElegibleDTO> noElegibles;
}