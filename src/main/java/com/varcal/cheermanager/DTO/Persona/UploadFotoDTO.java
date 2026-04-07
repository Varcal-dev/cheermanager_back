package com.varcal.cheermanager.DTO.Persona;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class UploadFotoDTO {
    private boolean exitoso;
    private String mensaje;
    private String nombreArchivo;
    private String rutaDescarga;
}
