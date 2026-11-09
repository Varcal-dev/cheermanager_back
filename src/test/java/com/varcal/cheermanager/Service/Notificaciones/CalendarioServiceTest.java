package com.varcal.cheermanager.Service.Notificaciones;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Eventos.Evento;
import com.varcal.cheermanager.Models.Horario_Asistencia.Horario;
import com.varcal.cheermanager.Models.Org_dep.DiaSemana;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.repository.Eventos.EventoRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.HorarioRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** C7: calendarios .ics de grupo y de eventos. */
class CalendarioServiceTest {

    final List<Horario> horarios = new ArrayList<>();
    final List<Evento> eventos = new ArrayList<>();
    final GrupoEntrenamiento grupo = new GrupoEntrenamiento();

    final CalendarioService service = new CalendarioService(
            Fakes.proxy(GrupoEntrenamientoRepository.class, new Repo().on("findById", a -> a[0].equals(3) ? Optional.of(grupo) : Optional.empty())),
            Fakes.proxy(HorarioRepository.class, new Repo().on("findByGrupoEntrenamientoId", a -> horarios)),
            Fakes.proxy(EventoRepository.class, new Repo().on("findAll", a -> eventos))) {
        @Override protected LocalDate hoy() { return LocalDate.of(2026, 11, 8); } // domingo
        @Override protected LocalDateTime ahora() { return LocalDateTime.of(2026, 11, 8, 12, 0); }
    };

    void horario(int id, int dia, String ini, String fin) {
        DiaSemana d = new DiaSemana(); d.setId(dia);
        Horario h = new Horario(); h.setId(id); h.setDia(d); h.setHoraInicio(LocalTime.parse(ini)); h.setHoraFin(LocalTime.parse(fin));
        horarios.add(h);
    }

    void evento(int id, String nombre, LocalDate fecha) {
        Evento e = new Evento(); e.setId(id); e.setNombre(nombre); e.setFecha(fecha); e.setUbicacion("Coliseo");
        eventos.add(e);
    }

    @Test
    void calendarioDelGrupoRepiteCadaSemana() {
        horarios.clear();
        grupo.setId(3); grupo.setNombre("Black Elite");
        horario(1, 2, "18:00:00", "20:00:00"); // martes
        horario(2, 6, "08:00:00", "11:00:00"); // sabado
        horario(3, 4, "20:00:00", "19:00:00"); // invalido: se ignora
        String ics = service.calendarioGrupo(3);
        assertTrue(ics.contains("DTSTART;TZID=America/Bogota:20261110T180000")); // primer martes desde el domingo 8
        assertTrue(ics.contains("RRULE:FREQ=WEEKLY;BYDAY=TU"));
        assertTrue(ics.contains("DTSTART;TZID=America/Bogota:20261114T080000")); // primer sabado
        assertTrue(ics.contains("BYDAY=SA"));
        assertEquals(2, ics.split("BEGIN:VEVENT", -1).length - 1);
        assertTrue(ics.contains("SUMMARY:Entrenamiento Black Elite"));
        assertEquals("entrenamientos-black-elite.ics", service.nombreArchivoGrupo(3));
        assertThrows(IllegalArgumentException.class, () -> service.calendarioGrupo(99));
    }

    @Test
    void calendarioDeEventosSoloFuturos() {
        eventos.clear();
        evento(1, "Copa pasada", LocalDate.of(2026, 10, 1));
        evento(2, "Copa Ratels", LocalDate.of(2026, 11, 21));
        evento(3, "Hoy mismo", LocalDate.of(2026, 11, 8));
        String ics = service.calendarioEventos();
        assertFalse(ics.contains("Copa pasada"));
        assertTrue(ics.contains("SUMMARY:Copa Ratels"));
        assertTrue(ics.contains("LOCATION:Coliseo"));
        assertTrue(ics.indexOf("Hoy mismo") < ics.indexOf("Copa Ratels")); // por fecha
    }
}
