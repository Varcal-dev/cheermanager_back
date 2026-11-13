package com.varcal.cheermanager.Service.Evaluaciones.Habilidades;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Evaluaciones.Habilidades.DeportistaHabilidad;
import com.varcal.cheermanager.Models.Evaluaciones.Habilidades.EvaluacionHabilidad;
import com.varcal.cheermanager.Models.Evaluaciones.Habilidades.EvaluacionHabilidadDriver;
import com.varcal.cheermanager.Models.Evaluaciones.Habilidades.HabilidadCatalogo;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Service.Evaluaciones.Habilidades.EvaluacionHabilidadService.Deduccion;
import com.varcal.cheermanager.Service.Evaluaciones.Habilidades.EvaluacionHabilidadService.Solicitud;
import com.varcal.cheermanager.repository.Evaluaciones.Habilidades.DeportistaHabilidadRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Habilidades.EvaluacionHabilidadDriverRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Habilidades.EvaluacionHabilidadRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Habilidades.HabilidadCatalogoRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** C1: evaluar una habilidad = valor inicial de la categoria - deducciones de sus drivers; actualiza el estado. */
class EvaluacionHabilidadServiceTest {

    List<EvaluacionHabilidad> evals;
    List<EvaluacionHabilidadDriver> drivers;
    List<DeportistaHabilidad> estados;

    static HabilidadCatalogo hab(int id, String nivel, String cat, boolean activa) {
        HabilidadCatalogo h = new HabilidadCatalogo();
        h.setId(id);
        h.setNivel(nivel);
        h.setCategoria(cat);
        h.setNombre("H" + id);
        h.setActivo(activa);
        return h;
    }

    EvaluacionHabilidadService servicio() {
        evals = new ArrayList<>();
        drivers = new ArrayList<>();
        estados = new ArrayList<>();
        Deportista d = new Deportista();
        d.setId(7);
        List<HabilidadCatalogo> cat = List.of(hab(1, "N1", "ELEVACION", true), hab(2, "N1", "GIMNASIA_CARRERA", true),
                hab(3, "N3", "LANZAMIENTO", true), hab(4, "N2", "ELEVACION", false), hab(5, "N2", "GIMNASIA_ESTATICA", true));
        Repo deportistas = new Repo().on("findById", a -> Optional.ofNullable(((Integer) a[0]) == 7 ? d : null));
        Repo catalogo = new Repo()
                .on("findById", a -> cat.stream().filter(h -> h.getId().equals(a[0])).findFirst())
                .on("findByActivoTrueOrderById", a -> cat.stream().filter(HabilidadCatalogo::getActivo).toList());
        Repo ev = new Repo()
                .on("save", a -> { EvaluacionHabilidad e = (EvaluacionHabilidad) a[0]; e.setId(evals.size() + 1); evals.add(e); return e; })
                .on("findByDeportistaIdAndHabilidadIdOrderByFechaDescIdDesc", a -> evals.stream()
                        .sorted((x, y) -> y.getFecha().compareTo(x.getFecha())).toList());
        Repo dr = new Repo()
                .on("save", a -> { drivers.add((EvaluacionHabilidadDriver) a[0]); return a[0]; })
                .on("findByEvaluacionHabilidadIdIn", a -> drivers);
        Repo st = new Repo()
                .on("findByDeportistaIdAndHabilidadId", a -> estados.stream()
                        .filter(x -> x.getHabilidad().getId().equals(a[1])).findFirst())
                .on("findByDeportistaId", a -> estados)
                .on("save", a -> { DeportistaHabilidad e = (DeportistaHabilidad) a[0]; if (!estados.contains(e)) estados.add(e); return e; });
        return new EvaluacionHabilidadService(Fakes.proxy(HabilidadCatalogoRepository.class, catalogo),
                Fakes.proxy(DeportistaHabilidadRepository.class, st), Fakes.proxy(EvaluacionHabilidadRepository.class, ev),
                Fakes.proxy(EvaluacionHabilidadDriverRepository.class, dr), Fakes.proxy(DeportistaRepository.class, deportistas));
    }

    static Solicitud sol(int habilidad, LocalDate fecha, Deduccion... d) {
        return new Solicitud(7, habilidad, fecha, null, List.of(d));
    }

    @Test
    void elevacion_sin_deducciones_vale_4_y_queda_dominada() {
        EvaluacionHabilidadService.Resultado r = servicio().evaluar(sol(1, null), 3);
        assertEquals(0, new BigDecimal("4.0").compareTo(r.puntaje()));
        assertEquals("DOMINADA", r.estado());
    }

    @Test
    void descuenta_01_por_nivel_de_cada_driver() {
        EvaluacionHabilidadService s = servicio();
        EvaluacionHabilidadService.Resultado r = s.evaluar(sol(1, null, new Deduccion("Flyer", 2), new Deduccion("Bases_Spotters", 1),
                new Deduccion("Transiciones", 3)), 3);
        assertEquals(0, new BigDecimal("0.6").compareTo(r.totalDeducciones()));
        assertEquals(0, new BigDecimal("3.4").compareTo(r.puntaje()));
        assertEquals("DOMINADA", r.estado());
        assertEquals(3, drivers.size());
        assertEquals(3, evals.get(0).getUsuarioId());
    }

    @Test
    void estados_segun_porcentaje_del_valor_inicial() {
        EvaluacionHabilidadService s = servicio();
        // 4.0 - 0.9 = 3.1 (77%) en desarrollo
        assertEquals("EN_DESARROLLO", s.evaluar(sol(1, null, new Deduccion("Flyer", 3), new Deduccion("Bases_Spotters", 3),
                new Deduccion("Transiciones", 3)), 1).estado());
        // 4.0 - 1.2 = 2.8 (70%) en desarrollo
        assertEquals("EN_DESARROLLO", servicio().evaluar(sol(1, null, new Deduccion("Flyer", 3), new Deduccion("Bases_Spotters", 3),
                new Deduccion("Transiciones", 3), new Deduccion("Sincronizacion", 3)), 1).estado());
    }

    @Test
    void lanzamiento_parte_de_2_y_altura_tope_01() {
        EvaluacionHabilidadService s = servicio();
        EvaluacionHabilidadService.Resultado r = s.evaluar(sol(3, null, new Deduccion("Altura", 1), new Deduccion("Flyer", 3)), 1);
        assertEquals(0, new BigDecimal("1.6").compareTo(r.puntaje()));
        assertThrows(IllegalArgumentException.class, () -> servicio().evaluar(sol(3, null, new Deduccion("Altura", 2)), 1));
    }

    @Test
    void driver_que_no_corresponde_a_la_categoria_se_rechaza() {
        assertThrows(IllegalArgumentException.class, () -> servicio().evaluar(sol(1, null, new Deduccion("Aterrizajes", 1)), 1));
        assertThrows(IllegalArgumentException.class, () -> servicio().evaluar(sol(2, null, new Deduccion("Flyer", 1)), 1));
        servicio().evaluar(sol(2, null, new Deduccion("Aterrizajes", 1), new Deduccion("Aproximacion", 0)), 1); // no lanza
    }

    @Test
    void rechaza_nivel_fuera_de_rango_driver_repetido_y_datos_invalidos() {
        assertThrows(IllegalArgumentException.class, () -> servicio().evaluar(sol(1, null, new Deduccion("Flyer", 4)), 1));
        assertThrows(IllegalArgumentException.class, () -> servicio().evaluar(sol(1, null, new Deduccion("Flyer", -1)), 1));
        assertThrows(IllegalArgumentException.class,
                () -> servicio().evaluar(sol(1, null, new Deduccion("Flyer", 1), new Deduccion("Flyer", 1)), 1));
        assertThrows(IllegalArgumentException.class, () -> servicio().evaluar(sol(1, com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().plusDays(1)), 1));
        assertThrows(IllegalArgumentException.class, () -> servicio().evaluar(new Solicitud(99, 1, null, null, null), 1));
        assertThrows(IllegalArgumentException.class, () -> servicio().evaluar(sol(99, null), 1));
        assertThrows(IllegalArgumentException.class, () -> servicio().evaluar(sol(4, null), 1)); // inactiva
    }

    @Test
    void reevaluar_actualiza_estado_cuenta_y_mejor_puntaje() {
        EvaluacionHabilidadService s = servicio();
        LocalDate hoy = com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy();
        s.evaluar(sol(1, hoy.minusDays(10), new Deduccion("Flyer", 3), new Deduccion("Bases_Spotters", 3),
                new Deduccion("Transiciones", 3), new Deduccion("Sincronizacion", 3)), 1); // 2.8
        s.evaluar(sol(1, hoy), 1); // 4.0
        assertEquals(1, estados.size());
        assertEquals("DOMINADA", estados.get(0).getEstado());
        assertEquals(2, estados.get(0).getTotalEvaluaciones());
        assertEquals(0, new BigDecimal("4.0").compareTo(estados.get(0).getMejorPuntaje()));
        // una evaluacion con fecha anterior no pisa el estado actual
        s.evaluar(sol(1, hoy.minusDays(30), new Deduccion("Flyer", 3), new Deduccion("Bases_Spotters", 3),
                new Deduccion("Transiciones", 3), new Deduccion("Sincronizacion", 3)), 1);
        assertEquals("DOMINADA", estados.get(0).getEstado());
        assertEquals(3, estados.get(0).getTotalEvaluaciones());
    }

    @Test
    void estado_del_deportista_resume_avance_por_nivel() {
        EvaluacionHabilidadService s = servicio();
        s.evaluar(sol(1, null), 1);
        s.evaluar(sol(2, null, new Deduccion("Aterrizajes", 3), new Deduccion("Aproximacion", 3), new Deduccion("Control_Cuerpo", 3),
                new Deduccion("Sincronizacion", 3)), 1); // 2.8 -> en desarrollo
        EvaluacionHabilidadService.EstadoDeportista e = s.estadoDeDeportista(7);
        assertEquals(2, e.habilidades().size());
        EvaluacionHabilidadService.ProgresoNivel n1 = e.porNivel().stream().filter(p -> p.nivel().equals("N1")).findFirst().get();
        assertEquals(2, n1.totalCatalogo());
        assertEquals(2, n1.evaluadas());
        assertEquals(1, n1.dominadas());
        // la habilidad inactiva (id 4, N2) no cuenta en el catalogo
        assertEquals(1, e.porNivel().stream().filter(p -> p.nivel().equals("N2")).findFirst().get().totalCatalogo());
    }

    @Test
    void historial_trae_las_deducciones_de_cada_evaluacion() {
        EvaluacionHabilidadService s = servicio();
        s.evaluar(sol(1, null, new Deduccion("Flyer", 2)), 1);
        List<EvaluacionHabilidadService.ItemHistorial> h = s.historial(7, 1);
        assertEquals(1, h.size());
        assertEquals("Flyer", h.get(0).deducciones().get(0).driver());
        assertEquals(0, new BigDecimal("3.8").compareTo(h.get(0).puntaje()));
    }

    @Test
    void catalogo_se_filtra_por_nivel_categoria_y_oculta_inactivas() {
        EvaluacionHabilidadService s = servicio();
        assertEquals(4, s.listarCatalogo(null, null, null).size());
        assertEquals(2, s.listarCatalogo("N1", null, null).size());
        assertEquals(1, s.listarCatalogo("n1", "elevacion", null).size());
        assertEquals(4, s.driversDe("elevacion").size());
        assertEquals(3, s.driversDe("LANZAMIENTO").size());
        assertThrows(IllegalArgumentException.class, () -> s.driversDe("saltos"));
    }
}
