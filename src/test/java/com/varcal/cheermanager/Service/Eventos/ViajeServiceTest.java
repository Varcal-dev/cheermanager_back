package com.varcal.cheermanager.Service.Eventos;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Eventos.Evento;
import com.varcal.cheermanager.Models.Eventos.RosterCompetencia;
import com.varcal.cheermanager.Models.Eventos.RosterIntegrante;
import com.varcal.cheermanager.Models.Eventos.ViajeAbono;
import com.varcal.cheermanager.Models.Eventos.ViajeCompetencia;
import com.varcal.cheermanager.Models.Eventos.ViajeParticipante;
import com.varcal.cheermanager.Models.Eventos.ViajeParticipante.Estado;
import com.varcal.cheermanager.Models.Eventos.ViajeParticipante.RolAcompanante;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.repository.Eventos.EventoRepository;
import com.varcal.cheermanager.repository.Eventos.RosterCompetenciaRepository;
import com.varcal.cheermanager.repository.Eventos.RosterIntegranteRepository;
import com.varcal.cheermanager.repository.Eventos.ViajeAbonoRepository;
import com.varcal.cheermanager.repository.Eventos.ViajeCompetenciaRepository;
import com.varcal.cheermanager.repository.Eventos.ViajeParticipanteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** C5: viaje a competencia. */
class ViajeServiceTest {

    final LocalDate fechaEvento = LocalDate.of(2026, 12, 5);
    final List<ViajeCompetencia> viajes = new ArrayList<>();
    final List<ViajeParticipante> parts = new ArrayList<>();
    final List<ViajeAbono> abonos = new ArrayList<>();
    final List<Evento> eventos = new ArrayList<>();
    final List<Deportista> deportistas = new ArrayList<>();
    final List<RosterCompetencia> rosters = new ArrayList<>();
    final List<RosterIntegrante> ints = new ArrayList<>();

    final ViajeService service = new ViajeService(
            Fakes.proxy(ViajeCompetenciaRepository.class, new Repo()
                    .on("save", a -> { ViajeCompetencia v = (ViajeCompetencia) a[0]; if (v.getId() == null) { v.setId(viajes.size() + 1); viajes.add(v); } return v; })
                    .on("findAll", a -> viajes)
                    .on("findById", a -> viajes.stream().filter(v -> v.getId().equals(a[0])).findFirst())
                    .on("findByEventoId", a -> viajes.stream().filter(v -> v.getEventoId().equals(a[0])).findFirst())),
            Fakes.proxy(ViajeParticipanteRepository.class, new Repo()
                    .on("save", a -> { ViajeParticipante p = (ViajeParticipante) a[0]; if (p.getId() == null) { p.setId(parts.size() + 100); parts.add(p); } return p; })
                    .on("findById", a -> parts.stream().filter(p -> p.getId().equals(a[0])).findFirst())
                    .on("deleteById", a -> { parts.removeIf(p -> p.getId().equals(a[0])); return null; })
                    .on("findByViajeIdOrderByIdAsc", a -> parts.stream().filter(p -> p.getViajeId().equals(a[0])).toList())),
            Fakes.proxy(ViajeAbonoRepository.class, new Repo()
                    .on("save", a -> { ViajeAbono x = (ViajeAbono) a[0]; abonos.add(x); return x; })
                    .on("findByParticipanteIdOrderByIdAsc", a -> abonos.stream().filter(x -> x.getParticipanteId().equals(a[0])).toList())),
            Fakes.proxy(EventoRepository.class, new Repo()
                    .on("findAll", a -> eventos)
                    .on("findById", a -> eventos.stream().filter(e -> e.getId().equals(a[0])).findFirst())),
            Fakes.proxy(DeportistaRepository.class, new Repo()
                    .on("findAll", a -> deportistas)
                    .on("findById", a -> deportistas.stream().filter(d -> d.getId().equals(a[0])).findFirst())),
            Fakes.proxy(RosterCompetenciaRepository.class, new Repo().on("findAll", a -> rosters)),
            Fakes.proxy(RosterIntegranteRepository.class, new Repo()
                    .on("findByRosterIdOrderByRolAscIdAsc", a -> ints.stream().filter(i -> i.getRosterId().equals(a[0])).toList()))) {
        @Override protected LocalDate hoy() { return LocalDate.of(2026, 11, 10); }
        @Override protected LocalDateTime ahora() { return LocalDateTime.of(2026, 11, 10, 9, 0); }
    };

    void reiniciar() {
        viajes.clear(); parts.clear(); abonos.clear(); eventos.clear(); deportistas.clear(); rosters.clear(); ints.clear();
        Evento e = new Evento(); e.setId(1); e.setNombre("Nacional"); e.setFecha(fechaEvento); eventos.add(e);
        dep(1, LocalDate.of(2012, 3, 1));   // menor (14)
        dep(2, LocalDate.of(2000, 1, 1));   // mayor
        dep(3, null);                        // sin fecha: se trata como menor
        RosterCompetencia r = new RosterCompetencia(); r.setId(1); r.setEventoId(1); r.setGrupoId(5); rosters.add(r);
        for (int i = 1; i <= 3; i++) { RosterIntegrante x = new RosterIntegrante(); x.setRosterId(1); x.setDeportistaId(i); ints.add(x); }
    }

    void dep(int id, LocalDate nac) {
        Persona p = new Persona(); p.setId(100 + id); p.setNombre("Dep" + id); p.setApellidos("X"); p.setFechaNacimiento(nac);
        Deportista d = new Deportista(); d.setId(id); d.setPersona(p); deportistas.add(d);
    }

    ViajeService.Solicitud solicitud(BigDecimal costo) {
        return new ViajeService.Solicitud("Bogota", LocalDate.of(2026, 12, 4), LocalDate.of(2026, 12, 7), "Parque", "Bus", "Hotel X", costo, null);
    }

    @Test
    void creaViajeUnoPorEventoYValidaFechas() {
        reiniciar();
        assertThrows(IllegalArgumentException.class, () -> service.crear(9, solicitud(null), 7));
        assertThrows(IllegalArgumentException.class, () -> service.crear(1, new ViajeService.Solicitud("Bogota", LocalDate.of(2026, 12, 7), LocalDate.of(2026, 12, 4), null, null, null, null, null), 7));
        assertThrows(IllegalArgumentException.class, () -> service.crear(1, new ViajeService.Solicitud("Bogota", LocalDate.of(2026, 12, 6), LocalDate.of(2026, 12, 8), null, null, null, null, null), 7)); // evento fuera del viaje
        assertThrows(IllegalArgumentException.class, () -> service.crear(1, solicitud(new BigDecimal("-1")), 7));
        ViajeService.Vista v = service.crear(1, solicitud(new BigDecimal("300000")), 7);
        assertEquals("ABIERTO", v.estado().name());
        assertThrows(IllegalStateException.class, () -> service.crear(1, solicitud(null), 7));
        assertEquals(1, service.opciones().get(0).viajeId());
    }

    @Test
    void cargaDelegacionDesdeRostersSinDuplicar() {
        reiniciar();
        service.crear(1, solicitud(null), 7);
        assertEquals(3, service.cargarDelegacion(1));
        assertEquals(0, service.cargarDelegacion(1));
        assertEquals(3, service.vista(1).resumen().deportistas());
        assertEquals(3, service.vista(1).resumen().pendientes());
    }

    @Test
    void menorSoloSeConfirmaConAutorizacion() {
        reiniciar();
        service.crear(1, solicitud(null), 7);
        service.cargarDelegacion(1);
        Integer menor = parts.get(0).getId(), mayor = parts.get(1).getId(), sinFecha = parts.get(2).getId();
        assertThrows(IllegalArgumentException.class, () -> service.actualizarParticipante(menor, Estado.CONFIRMADO, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.actualizarParticipante(sinFecha, Estado.CONFIRMADO, null, null));
        service.actualizarParticipante(mayor, Estado.CONFIRMADO, null, null); // mayor de edad no necesita carta
        ViajeService.Vista v = service.actualizarParticipante(menor, Estado.CONFIRMADO, true, null); // firma y confirma en un paso
        assertEquals(LocalDate.of(2026, 11, 10), parts.get(0).getFechaAutorizacion());
        assertEquals(2, v.resumen().confirmados());
        assertThrows(IllegalArgumentException.class, () -> service.actualizarParticipante(menor, Estado.CONFIRMADO, false, null)); // no se retira estando confirmado
        service.actualizarParticipante(menor, Estado.PENDIENTE, false, null);
        assertFalse(parts.get(0).isAutorizacionViaje());
    }

    @Test
    void abonosConSaldoYSinSobrepago() {
        reiniciar();
        service.crear(1, solicitud(null), 7);
        service.cargarDelegacion(1);
        Integer p = parts.get(1).getId();
        assertThrows(IllegalStateException.class, () -> service.abonar(p, new BigDecimal("1000"), null, 7)); // sin costo definido
        service.actualizar(1, solicitud(new BigDecimal("300000")));
        service.abonar(p, new BigDecimal("100000"), "primer abono", 7);
        assertThrows(IllegalArgumentException.class, () -> service.abonar(p, new BigDecimal("250000"), null, 7)); // supera el saldo
        assertThrows(IllegalArgumentException.class, () -> service.abonar(p, BigDecimal.ZERO, null, 7));
        ViajeService.Vista v = service.abonar(p, new BigDecimal("200000"), null, 7);
        ViajeService.Participante fila = v.participantes().get(1);
        assertEquals(0, fila.saldo().compareTo(BigDecimal.ZERO));
        assertEquals(0, v.resumen().abonadoTotal().compareTo(new BigDecimal("300000")));
        assertEquals(0, v.resumen().costoTotal().compareTo(new BigDecimal("900000"))); // 3 personas
        assertThrows(IllegalStateException.class, () -> service.quitarParticipante(p)); // tiene abonos
        service.actualizarParticipante(parts.get(0).getId(), Estado.NO_VA, null, null);
        assertEquals(0, service.vista(1).resumen().costoTotal().compareTo(new BigDecimal("600000"))); // quien no va no paga
        assertThrows(IllegalStateException.class, () -> service.abonar(parts.get(0).getId(), new BigDecimal("1000"), null, 7));
    }

    @Test
    void cerrarExigePendientesResueltosAcompananteYAutorizaciones() {
        reiniciar();
        service.crear(1, solicitud(new BigDecimal("300000")), 7);
        service.cargarDelegacion(1);
        assertThrows(IllegalArgumentException.class, () -> service.cerrar(1)); // pendientes
        service.actualizarParticipante(parts.get(0).getId(), Estado.CONFIRMADO, true, null); // menor
        service.actualizarParticipante(parts.get(1).getId(), Estado.CONFIRMADO, null, null);
        service.actualizarParticipante(parts.get(2).getId(), Estado.NO_VA, null, null);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.cerrar(1));
        assertTrue(e.getMessage().contains("acompanante"));
        service.agregarAcompanante(1, "Carlos Ruiz", "123", RolAcompanante.ENTRENADOR);
        assertThrows(IllegalArgumentException.class, () -> service.cerrar(1)); // acompanante pendiente
        service.actualizarParticipante(parts.get(3).getId(), Estado.CONFIRMADO, null, null);
        assertEquals("CERRADO", service.cerrar(1).estado().name());
        assertThrows(IllegalStateException.class, () -> service.agregarAcompanante(1, "Otro", null, RolAcompanante.OTRO));
        assertThrows(IllegalStateException.class, () -> service.actualizar(1, solicitud(null)));
        service.abonar(parts.get(1).getId(), new BigDecimal("50000"), null, 7); // los abonos siguen permitidos
        assertEquals("ABIERTO", service.reabrir(1).estado().name());
        assertThrows(IllegalStateException.class, () -> service.reabrir(1));
    }

    @Test
    void acompananteNoLlevaAutorizacionDeViaje() {
        reiniciar();
        service.crear(1, solicitud(null), 7);
        service.agregarAcompanante(1, "Carlos Ruiz", null, RolAcompanante.ENTRENADOR);
        assertThrows(IllegalArgumentException.class, () -> service.actualizarParticipante(parts.get(0).getId(), null, true, null));
        assertThrows(IllegalArgumentException.class, () -> service.agregarAcompanante(1, "C", null, RolAcompanante.OTRO));
        assertThrows(IllegalArgumentException.class, () -> service.agregarAcompanante(1, "Carlos", null, null));
    }

    @Test
    void sinRostersNoSeCargaDelegacion() {
        reiniciar();
        rosters.clear(); ints.clear();
        service.crear(1, solicitud(null), 7);
        assertThrows(IllegalStateException.class, () -> service.cargarDelegacion(1));
    }
}
