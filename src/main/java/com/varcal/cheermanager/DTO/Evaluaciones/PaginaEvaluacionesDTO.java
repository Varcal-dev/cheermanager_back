package com.varcal.cheermanager.DTO.Evaluaciones;

import java.util.List;

import lombok.Data;

@Data
public class PaginaEvaluacionesDTO {
    private List<EvaluacionResponseDTO> contenido;
    private long total;
    private int pagina;
    private int tamano;
    private int totalPaginas;
}
