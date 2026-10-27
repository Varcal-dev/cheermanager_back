package com.varcal.cheermanager.DTO.Notificaciones;

import java.util.List;

import lombok.Data;

/** Que enviar y por donde. Si no se marca ningun canal se usa el aviso dentro del sistema (el estandar). */
@Data
public class RecordatorioMoraRequestDTO {
    private Boolean sistema;
    private Boolean correo;
    private Boolean whatsapp;
    /** true = avisar aunque ya se haya avisado hace menos de dias_entre_avisos. */
    private Boolean forzar;
    /** Opcional: solo estas facturas (deben estar en mora). */
    private List<Integer> facturaIds;
}
