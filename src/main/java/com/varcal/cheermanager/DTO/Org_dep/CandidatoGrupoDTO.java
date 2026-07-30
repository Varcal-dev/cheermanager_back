package com.varcal.cheermanager.DTO.Org_dep;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Reemplaza el string opaco que devolvía verificar_elegibilidad_deportista_grupo.
// Trae el motivo ya calculado y estructurado para que el front pueda mostrarlo
// directamente (tooltip, badge, etc.) sin tener que parsear texto.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidatoGrupoDTO {
    private Integer deportistaId;
    private String nombreCompleto;
    private String genero;
    private String nivel;
    private LocalDate fechaNacimiento;
    private Integer edad;
    private boolean elegible;
    private String motivo; // null cuando elegible = true
}