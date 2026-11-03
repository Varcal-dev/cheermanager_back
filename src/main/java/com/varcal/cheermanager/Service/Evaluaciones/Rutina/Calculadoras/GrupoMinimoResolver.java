package com.varcal.cheermanager.Service.Evaluaciones.Rutina.Calculadoras;

import com.varcal.cheermanager.Models.Evaluaciones.Rutina.TablaCantidadNivel;

// Traduce la clave de "cuantos del equipo lo hacen" a un numero concreto de la tabla de
// cantidad (grupos o atletas, segun la tabla). Lo usan todas las calculadoras de escalon.
//
//   NINGUNO        -> 0  (el juez no marco nada: cae al escalon "piso" de la rubrica)
//   MENOS_MAYORIA  -> mayoria - 1  ("menos de la MAYORIA realiza...", escalon 2.0 de gimnasia Elite)
//   MAYORIA        -> mayoria
//   GRAN_PARTE     -> granParte
//   MAXIMO         -> maximo
//
// Un escalon con condicion de grupo null es un piso: se cumple siempre (umbral 0).
final class GrupoMinimoResolver {

    private GrupoMinimoResolver() {
    }

    static int resolver(String clave, TablaCantidadNivel tabla) {
        if (clave == null || clave.isBlank()) {
            return 0;
        }
        switch (clave.toUpperCase()) {
            case "NINGUNO":
                return 0;
            case "MENOS_MAYORIA":
                return Math.max(tabla.getMayoria() - 1, 0);
            case "MAYORIA":
                return tabla.getMayoria();
            case "GRAN_PARTE":
                return tabla.getGranParte();
            case "MAXIMO":
                return tabla.getMaximo();
            default:
                throw new RuntimeException("Valor de grupo desconocido: " + clave
                        + ". Debe ser NINGUNO, MENOS_MAYORIA, MAYORIA, GRAN_PARTE o MAXIMO.");
        }
    }
}
