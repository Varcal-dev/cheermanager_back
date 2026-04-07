package com.varcal.cheermanager.DTO.Persona;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class ValidarDocumentoDTO {
    private boolean disponible;
    private String mensaje;
}
