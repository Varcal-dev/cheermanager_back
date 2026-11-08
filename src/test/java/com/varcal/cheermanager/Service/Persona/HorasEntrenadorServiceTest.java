package com.varcal.cheermanager.Service.Persona;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Horario_Asistencia.Horario;
import com.varcal.cheermanager.Models.Horario_Asistencia.HorarioEntrenamiento;
import com.varcal.cheermanager.Models.Org_dep.DiaSemana;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenador;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.Models.Personas.Entrenador;
import com.varcal.cheermanager.Models.Personas.EstadoPersona;
import com.varcal.cheermanager.Models.Personas.HoraEntrenador;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Persona.HorasEntrenadorService.Periodo;
import com.varcal.cheermanager.Service.Persona.HorasEntrenadorService.Precarga;
import com.varcal.cheermanager.Service.Persona.HorasEntrenadorService.Solicitud;
import com.varcal.cheermanager.repository.Horario_Asistencia.HorarioEntrenamientoRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenadorRepository;
import com.varcal.cheermanager.repository.Personas.EntrenadorRepository;
import com.varcal.cheermanager.repository.Personas.HoraEntrenadorRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** C10: horas dictadas, manuales y precargadas desde los horarios. */
class HorasEntrenadorServiceTest {

    final LocalDate hoy = LocalDate.of(2026, 11, 8); // domingo
    final List<HoraEntrenador> horas = new ArrayList<>();
    final List<Entrenador> entrenadores = new ArrayList<>();
    final List<GrupoEntrenador> asignaciones = new ArrayList<>();
    final List<HorarioEntrenamiento> horarios = new ArrayList<>();

    final HorasEntrenadorService service = new HorasEntrenadorService(
            Fakes.proxy(HoraEntrenadorRepository.class, new Repo()
                    .on("save", a -> {
                        HoraEntrenador h = (HoraEntrenador) a[0];
                        if (h.getId() == null) {
                            h.setId(horas.size() + 1);
                            horas.add(h);
                        }
                        return h;
                    })
                    .on("findById", a -> horas.stream().filter(h -> h.getId().equals(a[0])).findFirst())
                    .on("deleteById", a -> {
                        horas.removeIf(h -> h.getId().equals(a[0]));
                        return null;
                    })
                    .on("findByFechaBetweenOrderByFechaDescIdDesc", a -> {
                        List<HoraEntrenador> r = new ArrayList<>();
                        for (HoraEntrenador h : horas)
                            if (!h.getFecha().isBefore((LocalDate) a[0]) && !h.getFecha().isAfter((LocalDate) a[1])) r.add(h);
                        return r;
                    })),
            Fakes.proxy(EntrenadorRepository.class, new Repo()
                    .on("findAll", a -> entrenadores)
                    .on("findById", a -> entrenadores.stream().filter(e -> e.getId().equals(a[0])).findFirst())),
            Fakes.proxy(GrupoEntrenadorRepository.class, new Repo().on("findAll", a -> asignaciones)),
            Fakes.proxy(HorarioEntrenamientoRepository.class, new Repo().on("findByGrupoEntrenamientoId", a -> horarios))) {
        @Override
        protected LocalDate hoy() {
            return hoy;
        }
    };

    Entrenador entrenador(int id, String estado) {
        Persona p = new Persona();
        p.setNombre("E" + id);
        p.setApellidos("Z");
        EstadoPersona e = new EstadoPersona();
        e.setEstado(estado);
        Entrenador t = new Entrenador();
        t.setId(id);
        t.setPersona(p);
        t.setEstado(e);
        entrenadores.add(t);
        return t;
    }

    void horario(int diaId, String ini, String fin) {
        DiaSemana d = new DiaSemana();
        d.setId(diaId);
        Horario h = new Horario();
        h.setDia(d);
        h.setHoraInicio(LocalTime.parse(ini));
        h.setHoraFin(LocalTime.parse(fin));
        HorarioEntrenamiento he = new HorarioEntrenamiento();
        he.setHorario(h);
        horarios.add(he);
    }

    void asignar(Entrenador e, int grupoId, LocalDate ini, LocalDate fin) {
        GrupoEntrenamiento g = new GrupoEntrenamiento();
        g.setId(grupoId);
        g.setNombre("G" + grupoId);
        GrupoEntrenador ge = new GrupoEntrenador();
        ge.setGrupo(g);
        ge.setEntrenador(e);
        ge.setFechaInicio(ini);
        ge.setFechaFin(fin);
        asignaciones.add(ge);
    }

    void reiniciar() {
        horas.clear();
        entrenadores.clear();
        asignaciones.clear();
        horarios.clear();
    }

    @Test
    void registraYValidaHorasManuales() {
        reiniciar();
        entrenador(1, "Activo");
        var r = service.registrar(new Solicitud(1, hoy, new BigDecimal("2.5"), null, " Clase extra "), 9);
        assertEquals("MANUAL", r.origen());
        assertEquals(new BigDecimal("2.50"), r.horas());
        assertEquals("Clase extra", r.descripcion());
        assertThrows(IllegalArgumentException.class, () -> service.registrar(new Solicitud(1, hoy, BigDecimal.ZERO, null, null), 9));
        assertThrows(IllegalArgumentException.class, () -> service.registrar(new Solicitud(1, hoy, new BigDecimal("13"), null, null), 9));
        assertThrows(IllegalArgumentException.class, () -> service.registrar(new Solicitud(1, hoy.plusDays(1), BigDecimal.ONE, null, null), 9));
        assertThrows(IllegalArgumentException.class, () -> service.registrar(new Solicitud(77, hoy, BigDecimal.ONE, null, null), 9));
        assertEquals(1, horas.size());
    }

    @Test
    void resumenPorEntrenadorYPeriodo() {
        reiniciar();
        entrenador(1, "Activo");
        entrenador(2, "Activo");
        service.registrar(new Solicitud(1, hoy, new BigDecimal("2"), null, null), 9);
        service.registrar(new Solicitud(1, hoy.minusDays(1), new BigDecimal("1.5"), null, null), 9);
        service.registrar(new Solicitud(2, hoy.minusDays(2), new BigDecimal("1"), null, null), 9);
        service.registrar(new Solicitud(2, hoy.minusDays(40), new BigDecimal("5"), null, null), 9); // fuera
        Periodo p = service.periodo(hoy.minusDays(7), hoy);
        assertEquals(new BigDecimal("4.50"), p.totalHoras());
        assertEquals(3, p.registros().size());
        assertEquals(1, p.totales().get(0).entrenadorId()); // el de mas horas primero
        assertEquals(new BigDecimal("3.50"), p.totales().get(0).horas());
        assertEquals(2, p.totales().get(0).sesiones());
        assertThrows(IllegalArgumentException.class, () -> service.periodo(hoy, hoy.minusDays(1)));
    }

    @Test
    void precargaDesdeHorariosSinDuplicar() {
        reiniciar();
        Entrenador e = entrenador(1, "Activo");
        asignar(e, 10, null, null);
        horario(2, "18:00:00", "20:00:00"); // martes 2h
        horario(4, "18:30:00", "20:00:00"); // jueves 1.5h
        // semana del lunes 2-nov al domingo 8-nov-2026: martes 3 y jueves 5
        Precarga p = service.precargar(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 8), 9);
        assertEquals(2, p.creadas());
        assertEquals(new BigDecimal("3.50"), p.horas());
        assertEquals("HORARIO", horas.get(0).getOrigen());
        Precarga otra = service.precargar(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 8), 9);
        assertEquals(0, otra.creadas());
        assertEquals(2, otra.omitidas());
        assertEquals(2, horas.size());
    }

    @Test
    void precargaRespetaEstadoVigenciaYLimites() {
        reiniciar();
        Entrenador activo = entrenador(1, "Activo");
        Entrenador inactivo = entrenador(2, "Inactivo");
        asignar(activo, 10, LocalDate.of(2026, 11, 4), null); // empieza el miercoles 4: el martes 3 no cuenta
        asignar(inactivo, 10, null, null);
        horario(2, "18:00:00", "20:00:00");
        horario(4, "18:00:00", "19:00:00");
        Precarga p = service.precargar(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 8), 9);
        assertEquals(1, p.creadas()); // solo el jueves del activo
        assertEquals(new BigDecimal("1.00"), p.horas());
        assertThrows(IllegalArgumentException.class, () -> service.precargar(hoy, hoy.plusDays(1), 9));
        assertThrows(IllegalArgumentException.class, () -> service.precargar(hoy.minusDays(100), hoy, 9));
    }

    @Test
    void eliminaRegistros() {
        reiniciar();
        entrenador(1, "Activo");
        var r = service.registrar(new Solicitud(1, hoy, BigDecimal.ONE, null, null), 9);
        service.eliminar(r.id());
        assertTrue(horas.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service.eliminar(r.id()));
    }
}
