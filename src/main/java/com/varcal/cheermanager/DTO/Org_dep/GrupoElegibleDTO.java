// src/main/java/com/varcal/cheermanager/DTO/Org_dep/GrupoElegibleDTO.java
package com.varcal.cheermanager.DTO.Org_dep;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Espejo de CandidatoGrupoDTO, pero visto desde el deportista: en vez de
// "¿qué deportistas entran en este grupo?", responde "¿en qué grupos
// entra este deportista?". Misma regla de elegibilidad (año de nacimiento
// de la categoría + cupo), solo que aplicada en la dirección inversa.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GrupoElegibleDTO {
    private Integer grupoId;
    private String nombreGrupo;
    private String tipoGrupo;
    private String categoriaNivel;
    private Integer cupoActual;
    private Integer cupoMaximo; // null = sin límite definido
    private boolean elegible;
    private String motivo; // null cuando elegible = true
}