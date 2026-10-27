package com.varcal.cheermanager.DTO.Notificaciones;

import lombok.Data;

@Data
public class RecordatorioConfigDTO {
    private Boolean automatico;
    private Boolean sistema;
    private Boolean correo;
    private Boolean whatsapp;
    private Integer diasEntreAvisos;
}
