package com.varcal.cheermanager.Service.Org_dep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.varcal.cheermanager.DTO.Org_dep.InscripcionActualizacionDTO;
import com.varcal.cheermanager.Models.Financiero.PlanPago;
import com.varcal.cheermanager.Models.Historiales.HistorialPlanesDeportista;
import com.varcal.cheermanager.Models.Org_dep.Inscripcion;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Service.Historiales.HistorialPlanesDeportistaService;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
import com.varcal.cheermanager.repository.Financiero.PlanPagoRepository;
import com.varcal.cheermanager.repository.Org_dep.InscripcionRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/**
 * SEC-02: InscripcionService.actualizarInscripcion ahora recibe InscripcionActualizacionDTO. Solo cambia lo que el DTO
 * trae; los planes se resuelven con findById (filtrado por club), asi que un plan de otro club "no existe".
 */
class InscripcionActualizarTest {

    static final class HistorialFalso extends HistorialPlanesDeportistaService {
        final List<String> cambios = new ArrayList<>();

        @Override
        public HistorialPlanesDeportista registrarCambioDePlan(Integer deportistaId, Integer planPagoNuevoId,
                LocalDate fechaCambio, String motivo) {
            cambios.add(deportistaId + ":" + planPagoNuevoId);
            return null;
        }
    }

    /** Planes DEL CLUB: findById solo encuentra estos, como el filtro de tenant (uno de otro club responde vacio). */
    final List<PlanPago> planesDelClub = new ArrayList<>();
    Inscripcion existente;
    Repo inscripciones;
    HistorialFalso historial;
    InscripcionService service;

    static PlanPago plan(int id) {
        PlanPago p = new PlanPago();
        p.setId(id);
        return p;
    }

    @BeforeEach
    void preparar() {
        planesDelClub.clear();
        planesDelClub.add(plan(3));
        planesDelClub.add(plan(4));
        planesDelClub.add(plan(8));

        Deportista d = new Deportista();
        d.setId(7);
        existente = new Inscripcion();
        existente.setId(9);
        existente.setDeportista(d);
        existente.setPlanPago(plan(3));
        existente.setFechaInscripcion(LocalDate.of(2026, 1, 10));
        existente.setFechaVencimiento(LocalDate.of(2026, 2, 10));
        existente.setEstado(Inscripcion.EstadoInscripcion.Activa);

        inscripciones = new Repo()
                .on("findById", a -> a[0].equals(9) ? Optional.of(existente) : Optional.empty())
                .on("save", a -> a[0]);
        historial = new HistorialFalso();
        service = new InscripcionService();
        ReflectionTestUtils.setField(service, "inscripcionRepository", Fakes.proxy(InscripcionRepository.class, inscripciones));
        ReflectionTestUtils.setField(service, "planPagoRepository", Fakes.proxy(PlanPagoRepository.class, new Repo()
                .on("findById", a -> planesDelClub.stream().filter(p -> p.getId().equals(a[0])).findFirst())));
        ReflectionTestUtils.setField(service, "facturaRepository", Fakes.proxy(FacturaRepository.class, new Repo()
                .on("findByInscripcionId", a -> List.of())));
        ReflectionTestUtils.setField(service, "historialPlanesDeportistaService", historial);
    }

    static InscripcionActualizacionDTO cambios(Integer planPagoId, Integer planMensualidadId, String estado) {
        InscripcionActualizacionDTO dto = new InscripcionActualizacionDTO();
        if (planPagoId != null) {
            dto.setPlanPago(ref(planPagoId));
        }
        if (planMensualidadId != null) {
            dto.setPlanMensualidad(ref(planMensualidadId));
        }
        dto.setFechaInscripcion(LocalDate.of(2026, 11, 27));
        dto.setFechaVencimiento(LocalDate.of(2026, 12, 27));
        dto.setEstado(estado);
        return dto;
    }

    static InscripcionActualizacionDTO.PlanRef ref(int id) {
        InscripcionActualizacionDTO.PlanRef r = new InscripcionActualizacionDTO.PlanRef();
        r.setId(id);
        return r;
    }

    @Test
    void unPlanQueNoEsDelClubSeRechazaYNoSeGuardaNada() {
        var e = assertThrows(RuntimeException.class, () -> service.actualizarInscripcion(9, cambios(99, null, "Activa")));
        assertEquals("Plan no encontrado", e.getMessage());
        assertEquals(0, inscripciones.veces("save"));
        assertEquals(0, historial.cambios.size());
    }

    @Test
    void unPlanDeMensualidadQueNoEsDelClubSeRechaza() {
        var e = assertThrows(RuntimeException.class, () -> service.actualizarInscripcion(9, cambios(3, 99, "Activa")));
        assertEquals("Plan de mensualidad no encontrado", e.getMessage());
        assertEquals(0, inscripciones.veces("save"));
    }

    @Test
    void conElMismoPlanActualizaFechasYEstadoSinRegistrarHistorial() {
        Inscripcion r = service.actualizarInscripcion(9, cambios(3, null, "Inactiva"));
        assertEquals(3, r.getPlanPago().getId());
        assertEquals(LocalDate.of(2026, 11, 27), r.getFechaInscripcion());
        assertEquals(LocalDate.of(2026, 12, 27), r.getFechaVencimiento());
        assertEquals(Inscripcion.EstadoInscripcion.Inactiva, r.getEstado());
        assertEquals(1, inscripciones.veces("save"));
        assertEquals(0, historial.cambios.size());
    }

    @Test
    void conOtroPlanLoCambiaYDejaConstanciaEnElHistorial() {
        Inscripcion r = service.actualizarInscripcion(9, cambios(4, 8, "Activa"));
        assertEquals(4, r.getPlanPago().getId());
        assertEquals(8, r.getPlanMensualidad().getId());
        assertEquals(List.of("7:4"), historial.cambios);
    }

    @Test
    void sinPlanesEnElCuerpoNoTocaLosPlanes() {
        existente.setPlanMensualidad(plan(8));
        Inscripcion r = service.actualizarInscripcion(9, cambios(null, null, "Activa"));
        assertEquals(3, r.getPlanPago().getId());
        assertEquals(8, r.getPlanMensualidad().getId());
        assertEquals(0, historial.cambios.size());
    }

    @Test
    void unaInscripcionQueNoExisteEnElClubDaNoEncontrada() {
        var e = assertThrows(RuntimeException.class, () -> service.actualizarInscripcion(500, cambios(3, null, "Activa")));
        assertEquals("Inscripción no encontrada", e.getMessage());
    }
}
