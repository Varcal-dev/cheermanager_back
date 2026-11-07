package com.varcal.cheermanager.Service.Evaluaciones.Habilidades;

import java.math.BigDecimal;
import java.util.List;

/**
 * Drivers de ejecucion por categoria de habilidad (rubrica USASF/FEDECOLCHEER): son la forma de ejecutar la
 * habilidad y lo que se puntua. Cada driver descuenta 0.1 por nivel (1 = menor, 2 = multiples, 3 = generalizado),
 * con tope de 0.3, salvo Altura (lanzamientos), que tiene tope de 0.1.
 */
public final class DriversHabilidad {

    private DriversHabilidad() {
    }

    public static final String ELEVACION = "ELEVACION";
    public static final String GIMNASIA_ESTATICA = "GIMNASIA_ESTATICA";
    public static final String GIMNASIA_CARRERA = "GIMNASIA_CARRERA";
    public static final String LANZAMIENTO = "LANZAMIENTO";

    /** Descuento por cada nivel de deduccion. */
    public static final BigDecimal PASO = new BigDecimal("0.1");

    public record Driver(String nombre, String etiqueta, int maxNivel) {
    }

    private static final List<Driver> ELEVACIONES = List.of(
            new Driver("Flyer", "Flyer", 3), new Driver("Bases_Spotters", "Bases / Spotters", 3),
            new Driver("Transiciones", "Transiciones", 3), new Driver("Sincronizacion", "Sincronización", 3));
    private static final List<Driver> GIMNASIA = List.of(
            new Driver("Aproximacion", "Aproximación", 3), new Driver("Control_Cuerpo", "Control del cuerpo", 3),
            new Driver("Aterrizajes", "Aterrizajes", 3), new Driver("Sincronizacion", "Sincronización", 3));
    private static final List<Driver> LANZAMIENTOS = List.of(
            new Driver("Flyer", "Flyer", 3), new Driver("Bases_Spotters", "Bases / Spotters", 3),
            new Driver("Altura", "Altura del lanzamiento", 1));

    public static List<Driver> de(String categoria) {
        if (ELEVACION.equals(categoria)) return ELEVACIONES;
        if (GIMNASIA_ESTATICA.equals(categoria) || GIMNASIA_CARRERA.equals(categoria)) return GIMNASIA;
        if (LANZAMIENTO.equals(categoria)) return LANZAMIENTOS;
        throw new IllegalArgumentException("Categoría de habilidad desconocida: " + categoria);
    }

    /** Valor inicial de ejecucion: 4.0 en elevaciones y gimnasia, 2.0 en lanzamientos. */
    public static BigDecimal valorInicial(String categoria) {
        de(categoria);
        return LANZAMIENTO.equals(categoria) ? new BigDecimal("2.0") : new BigDecimal("4.0");
    }
}
