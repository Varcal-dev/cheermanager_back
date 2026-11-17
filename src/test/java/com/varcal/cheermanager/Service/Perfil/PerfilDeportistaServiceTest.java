package com.varcal.cheermanager.Service.Perfil;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.DTO.Financiero.FacturaResponseDTO;
import com.varcal.cheermanager.DTO.Financiero.PagoResponseDTO;
import com.varcal.cheermanager.Models.Evaluaciones.CriterioEvaluacion;
import com.varcal.cheermanager.Models.Evaluaciones.Evaluacion;
import com.varcal.cheermanager.Models.Evaluaciones.Habilidades.EvaluacionHabilidad;
import com.varcal.cheermanager.Models.Evaluaciones.Habilidades.HabilidadCatalogo;
import com.varcal.cheermanager.Models.Evaluaciones.TipoCategoria;
import com.varcal.cheermanager.Models.Horario_Asistencia.Asistencia;
import com.varcal.cheermanager.Models.Horario_Asistencia.EstadoAsistencia;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.DocumentoDeportista;
import com.varcal.cheermanager.Models.Personas.EstadoPersona;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Evaluaciones.Habilidades.EvaluacionHabilidadService;
import com.varcal.cheermanager.Service.Evaluaciones.Habilidades.EvaluacionHabilidadService.ProgresoNivel;
import com.varcal.cheermanager.Service.Perfil.PerfilDeportistaService.Perfil;
import com.varcal.cheermanager.Service.Ranking.RankingService;
import com.varcal.cheermanager.repository.Evaluaciones.EvaluacionRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Habilidades.EvaluacionHabilidadRepository;
import com.varcal.cheermanager.repository.Financiero.PlanPagoRepository;
import com.varcal.cheermanager.repository.Historiales.HistorialPlanesDeportistaRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.AsistenciaRepository;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;
import com.varcal.cheermanager.repository.Org_dep.NivelRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.repository.Personas.DocumentoDeportistaRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** Perfil 360: que ve cada puerta (club / portal), la regla de facturacion y los calculos de asistencia y evolucion. */
class PerfilDeportistaServiceTest {

    static final LocalDate HOY = LocalDate.of(2026, 11, 16);

    final List<Asistencia> asistencias = new ArrayList<>();
    final List<Evaluacion> evaluaciones = new ArrayList<>();
    final List<EvaluacionHabilidad> evalHabilidad = new ArrayList<>();
    final List<DocumentoDeportista> documentos = new ArrayList<>();
    final List<FacturaResponseDTO> facturas = new ArrayList<>();
    final List<PagoResponseDTO> pagos = new ArrayList<>();
    final List<RankingService.AprendizajeFila> filasRanking = new ArrayList<>();
    boolean rankingConsultado = false;

    final Deportista deportista = deportista(LocalDate.of(2012, 5, 1));

    static Deportista deportista(LocalDate nacimiento) {
        Persona p = new Persona();
        p.setId(70);
        p.setNombre("Ana");
        p.setApellidos("Pérez");
        p.setFechaNacimiento(nacimiento);
        EstadoPersona ep = new EstadoPersona();
        ep.setEstado("Activo");
        Deportista d = new Deportista();
        d.setId(7);
        d.setPersona(p);
        d.setEstado(ep);
        d.setFechaRegistro(LocalDate.of(2025, 11, 1));
        return d;
    }

    final PerfilDeportistaService service = new PerfilDeportistaService(
            Fakes.proxy(DeportistaRepository.class,
                    new Repo().on("findById", a -> Optional.ofNullable(((Integer) a[0]) == 7 ? deportista : null))),
            Fakes.proxy(DeportistaPerteneceGrupoRepository.class,
                    new Repo().on("findByDeportistaIdAndFechaFinIsNull", a -> List.of())),
            Fakes.proxy(NivelRepository.class, new Repo().on("findById", a -> Optional.empty())),
            Fakes.proxy(HistorialPlanesDeportistaRepository.class,
                    new Repo().on("findByDeportistaIdAndFechaFinIsNull", a -> Optional.empty())),
            Fakes.proxy(PlanPagoRepository.class, new Repo().on("findById", a -> Optional.empty())),
            Fakes.proxy(DocumentoDeportistaRepository.class,
                    new Repo().on("findByDeportistaIdOrderByIdDesc", a -> documentos)),
            Fakes.proxy(AsistenciaRepository.class,
                    new Repo().on("findByDeportistaIdAndFechaBetweenOrderByFechaDesc", a -> asistencias)),
            Fakes.proxy(EvaluacionRepository.class,
                    new Repo().on("findByDeportistaIdOrderByFechaDesc", a -> evaluaciones)),
            Fakes.proxy(EvaluacionHabilidadRepository.class,
                    new Repo().on("findByDeportistaIdOrderByFechaAscIdAsc", a -> evalHabilidad)),
            null, null, null, null) {
        @Override
        protected LocalDate hoy() {
            return HOY;
        }

        @Override
        protected List<FacturaResponseDTO> facturasDe(Integer personaId) {
            return facturas;
        }

        @Override
        protected List<PagoResponseDTO> pagosDe(Integer personaId) {
            return pagos;
        }

        @Override
        protected EvaluacionHabilidadService.EstadoDeportista habilidadesDe(Integer deportistaId) {
            return new EvaluacionHabilidadService.EstadoDeportista(List.of(),
                    List.of(new ProgresoNivel("N2", 10, 0, 0), new ProgresoNivel("N1", 10, 10, 10)));
        }

        @Override
        protected RankingService.RankingHabilidades rankingHabilidadesAnioActual() {
            rankingConsultado = true;
            return new RankingService.RankingHabilidades(2026, null, 0, new int[12], filasRanking);
        }
    };

    // ---- ayudantes ----

    void asistencia(String fecha, String estado) {
        Asistencia a = new Asistencia();
        a.setFecha(LocalDate.parse(fecha));
        EstadoAsistencia e = new EstadoAsistencia();
        e.setEstado(estado);
        a.setEstadoAsistencia(e);
        asistencias.add(a); // el repositorio las devuelve de la mas reciente a la mas antigua
    }

    FacturaResponseDTO factura(String saldo, String estado, String vence, boolean anulada) {
        FacturaResponseDTO f = new FacturaResponseDTO();
        f.setId(facturas.size() + 1);
        f.setNumeroFactura("F-" + f.getId());
        f.setSaldoPendiente(new BigDecimal(saldo));
        f.setEstadoCuenta(estado);
        f.setFechaVencimiento(LocalDate.parse(vence));
        f.setAnulada(anulada);
        facturas.add(f);
        return f;
    }

    void evaluacion(String fecha, String categoria, int puntaje) {
        TipoCategoria c = new TipoCategoria();
        c.setCategoria(categoria);
        CriterioEvaluacion cr = new CriterioEvaluacion();
        cr.setCategoria(c);
        Evaluacion e = new Evaluacion();
        e.setFecha(LocalDate.parse(fecha));
        e.setPuntajeObtenido(puntaje);
        e.setCriterioEvaluacion(cr);
        evaluaciones.add(e);
    }

    void evalHabilidad(int habilidadId, String fecha, String puntaje) {
        HabilidadCatalogo h = new HabilidadCatalogo();
        h.setId(habilidadId);
        EvaluacionHabilidad e = new EvaluacionHabilidad();
        e.setHabilidad(h);
        e.setFecha(LocalDate.parse(fecha));
        e.setValorInicial(new BigDecimal("1.0"));
        e.setPuntaje(new BigDecimal(puntaje));
        evalHabilidad.add(e);
    }

    // ---- regla de facturacion ----

    @Test
    void la_facturacion_la_ve_el_acudiente_o_el_deportista_mayor_de_edad() {
        LocalDate menor = HOY.minusYears(17);
        assertTrue(PerfilDeportistaService.puedeVerFacturacion(true, menor, HOY), "el acudiente siempre");
        assertFalse(PerfilDeportistaService.puedeVerFacturacion(false, menor, HOY), "el menor no");
        assertTrue(PerfilDeportistaService.puedeVerFacturacion(false, HOY.minusYears(18), HOY), "cumple 18 hoy");
        assertFalse(PerfilDeportistaService.puedeVerFacturacion(false, HOY.minusYears(18).plusDays(1), HOY),
                "le falta un dia para los 18");
        assertFalse(PerfilDeportistaService.puedeVerFacturacion(false, null, HOY), "sin fecha de nacimiento: se oculta");
    }

    @Test
    void portal_deportista_menor_no_recibe_facturacion_ni_ranking() {
        factura("100000", "EN_MORA", "2026-11-01", false);
        Perfil p = service.paraPortal(deportista, false);
        assertNull(p.facturacion());
        assertNotNull(p.notaFacturacion());
        assertNotNull(p.estado());
        assertNotNull(p.asistencia());
        assertNotNull(p.evolucion());
        assertNull(p.evolucion().posicion(), "el portal nunca compara con otros atletas");
        assertFalse(rankingConsultado);
    }

    @Test
    void portal_acudiente_recibe_facturacion_y_tampoco_ranking() {
        factura("100000", "EN_MORA", "2026-11-01", false);
        Perfil p = service.paraPortal(deportista, true);
        assertNotNull(p.facturacion());
        assertNull(p.notaFacturacion());
        assertNull(p.evolucion().posicion());
        assertFalse(rankingConsultado);
    }

    // ---- puerta del club ----

    @Test
    void club_sin_permisos_de_seccion_solo_recibe_el_estado() {
        Perfil p = service.paraClub(7, Set.of("ver_deportista"));
        assertNotNull(p.estado());
        assertNull(p.facturacion());
        assertNull(p.asistencia());
        assertNull(p.evolucion());
        assertThrows(IllegalArgumentException.class, () -> service.paraClub(99, Set.of("ver_deportista")));
    }

    @Test
    void club_sin_ver_pago_recibe_facturacion_pero_no_los_pagos() {
        factura("50000", "PENDIENTE", "2026-11-25", false);
        PagoResponseDTO pago = new PagoResponseDTO();
        pago.setFecha(HOY);
        pago.setMonto(new BigDecimal("30000"));
        pago.setEstado("Pagado");
        pagos.add(pago);
        Perfil sinPago = service.paraClub(7, Set.of("ver_deportista", "ver_factura"));
        assertNotNull(sinPago.facturacion());
        assertNull(sinPago.facturacion().ultimosPagos());
        Perfil conPago = service.paraClub(7, Set.of("ver_deportista", "ver_factura", "ver_pago"));
        assertEquals(1, conPago.facturacion().ultimosPagos().size());
    }

    @Test
    void club_con_ver_evaluaciones_recibe_la_posicion_en_el_ranking() {
        filasRanking.add(new RankingService.AprendizajeFila(3, "Otra", 5, List.of()));
        filasRanking.add(new RankingService.AprendizajeFila(7, "Ana Pérez", 2, List.of()));
        filasRanking.add(new RankingService.AprendizajeFila(9, "Otro", 2, List.of()));
        Perfil p = service.paraClub(7, Set.of("ver_deportista", "ver_evaluaciones"));
        assertEquals(2, p.evolucion().posicion().posicion(), "empata en cantidad con otro: queda segunda");
        assertEquals(2, p.evolucion().posicion().aprendidas());
        assertEquals(3, p.evolucion().posicion().deportistasConAprendizaje());
    }

    // ---- facturacion ----

    @Test
    void facturacion_suma_saldos_ignora_anuladas_y_separa_la_mora() {
        factura("100000", "EN_MORA", "2026-11-01", false);
        factura("40000", "ABONO_PARCIAL", "2026-11-25", false);
        factura("70000", "PENDIENTE", "2026-12-05", false);
        factura("999999", "ANULADA", "2026-10-01", true);
        factura("0", "PAGADA", "2026-10-10", false);
        var f = service.paraPortal(deportista, true).facturacion();
        assertTrue(Fakes.igual("210000", f.saldoPendiente()));
        assertEquals(1, f.facturasEnMora());
        assertTrue(Fakes.igual("100000", f.montoEnMora()));
        assertEquals(LocalDate.of(2026, 11, 25), f.proximoVencimiento());
        assertEquals(3, f.pendientes().size());
        assertEquals("F-1", f.pendientes().get(0).numero(), "la mas vencida primero");
    }

    // ---- asistencia ----

    @Test
    void asistencia_calcula_porcentajes_y_racha() {
        // de la mas reciente a la mas antigua
        asistencia("2026-11-14", "Presente");
        asistencia("2026-11-12", "Tarde");
        asistencia("2026-11-10", "Justificado"); // no rompe la racha ni suma
        asistencia("2026-11-07", "Presente");
        asistencia("2026-11-05", "Ausente"); // corta la racha
        asistencia("2026-11-03", "Presente");
        asistencia("2026-10-20", "Presente");
        var a = service.paraPortal(deportista, true).asistencia();
        assertEquals(3, a.rachaActual());
        assertEquals(6, a.totalMes());
        assertEquals(3, a.presentesMes());
        assertEquals(50, a.porcentajeMes());
        assertEquals(7, a.total90d());
        assertEquals(4, a.presentes90d());
        assertEquals(1, a.tardes90d());
        assertEquals(1, a.ausentes90d());
        assertEquals(6, a.meses().size());
        assertEquals("2026-11", a.meses().get(5).mes());
        assertNull(a.meses().get(0).porcentaje(), "un mes sin sesiones no tiene porcentaje");
    }

    // ---- estado ----

    @Test
    void estado_pide_certificado_y_eps_aunque_falten_y_marca_los_vencidos() {
        DocumentoDeportista cert = new DocumentoDeportista();
        cert.setTipo(DocumentoDeportista.Tipo.CERTIFICADO_MEDICO);
        cert.setFechaVencimiento(HOY.minusDays(3));
        documentos.add(cert);
        var e = service.paraPortal(deportista, true).estado();
        assertEquals("CERTIFICADO_MEDICO", e.vencimientos().get(0).tipo());
        assertEquals("VENCIDO", e.vencimientos().get(0).estado());
        assertEquals("EPS", e.vencimientos().get(1).tipo());
        assertEquals("SIN_REGISTRO", e.vencimientos().get(1).estado());
        assertEquals(2, e.vencimientos().size(), "seguro y licencia solo aparecen si ya fueron registrados");
        assertEquals(14, e.edad());
        assertEquals(12, e.antiguedadMeses());
    }

    // ---- evolucion ----

    @Test
    void evolucion_promedia_por_fecha_y_categoria() {
        evaluacion("2026-10-01", "Gimnasia", 60);
        evaluacion("2026-10-01", "Saltos", 80);
        evaluacion("2026-11-01", "Gimnasia", 90);
        var ev = service.paraPortal(deportista, true).evolucion();
        assertEquals(3, ev.evaluaciones());
        assertEquals(76.7, ev.promedioGeneral());
        assertEquals(2, ev.serie().size());
        assertEquals(70.0, ev.serie().get(0).promedio());
        assertEquals(90.0, ev.serie().get(1).promedio());
        assertEquals(2, ev.porCategoria().size());
        assertEquals(75.0, ev.porCategoria().get(0).promedio());
    }

    @Test
    void habilidades_aprendidas_cuentan_solo_la_primera_vez_que_se_domina() {
        evalHabilidad(1, "2026-08-10", "0.5"); // aun no dominada (< 0.85)
        evalHabilidad(1, "2026-09-12", "0.9"); // la aprende en septiembre
        evalHabilidad(1, "2026-11-02", "1.0"); // ya la sabia: no cuenta otra vez
        evalHabilidad(2, "2026-11-03", "0.85"); // justo en el umbral: dominada
        var ev = service.paraPortal(deportista, true).evolucion();
        assertEquals(2, ev.habilidades().aprendidasAnio());
        assertEquals(1, ev.aprendidasPorMes().stream().filter(m -> m.mes().equals("2026-09")).findFirst().get().cantidad());
        assertEquals(1, ev.aprendidasPorMes().stream().filter(m -> m.mes().equals("2026-11")).findFirst().get().cantidad());
        assertEquals("N2", ev.habilidades().nivelEnCurso(), "N1 esta completo: el nivel en curso es N2");
        assertEquals(0, ev.habilidades().avanceNivelEnCurso());
    }
}
