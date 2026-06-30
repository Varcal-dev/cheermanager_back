package com.varcal.cheermanager.Service.Evaluaciones.Rutina.Calculadoras;

import com.varcal.cheermanager.DTO.Evaluaciones.Rutina.EvaluacionRutinaDTO.RegistroSubCriterioDTO;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.NivelCompetencia;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.RegistroSubCriterio;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.SubCriterioRubrica;

// Contrato común para las 5 calculadoras (una por SubCriterioRubrica.TipoCalculoSubCriterio).
// Cada calculadora recibe los datos crudos que ingresó el juez (en el DTO),
// la configuración del nivel correspondiente, y devuelve un RegistroSubCriterio
// ya resuelto (con puntajeBase, puntajeDrivers, puntajeFinal llenos).
//
// EvaluacionRutinaService elige qué calculadora invocar mirando
// subCriterio.getTipoCalculo() — ver el mapa de inyección en ese service.
public interface CalculadoraSubCriterio {

    RegistroSubCriterio calcular(RegistroSubCriterioDTO dto, SubCriterioRubrica subCriterio,
            NivelCompetencia nivel, Integer cantidadAtletas);

    SubCriterioRubrica.TipoCalculoSubCriterio getTipo();
}