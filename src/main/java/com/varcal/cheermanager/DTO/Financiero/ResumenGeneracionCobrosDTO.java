package com.varcal.cheermanager.DTO.Financiero;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

// Respuesta de POST /api/inscripciones/generar-cobros-mensualidad. Deja ver
// exactamente qué pasó al correr el proceso: cuántas facturas nuevas se
// generaron, cuántas inscripciones ya estaban facturadas para ese período
// (se saltaron, no se duplicó nada) y si algo falló puntualmente.
@Data
public class ResumenGeneracionCobrosDTO {
    private String periodo;
    private int facturasGeneradas;
    private int yaFacturadas;
    private List<String> errores = new ArrayList<>();
}