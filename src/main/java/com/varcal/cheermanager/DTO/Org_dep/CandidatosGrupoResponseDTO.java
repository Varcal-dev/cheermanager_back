package com.varcal.cheermanager.DTO.Org_dep;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Respuesta de GET /api/grupos/{grupoId}/candidatos.
// Sustituye al patrón N+1 donde el front pedía, por cada deportista inscrito,
// un GET individual a /verificar-elegibilidad. Ahora es una sola llamada que
// ya trae todo separado y con el cupo del grupo calculado.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidatosGrupoResponseDTO {
    private String grupoNombre;
    private Integer cupoActual;
    private Integer cupoMaximo; // null = la categoría no tiene límite definido
    private List<CandidatoGrupoDTO> elegibles;
    private List<CandidatoGrupoDTO> noElegibles;
}