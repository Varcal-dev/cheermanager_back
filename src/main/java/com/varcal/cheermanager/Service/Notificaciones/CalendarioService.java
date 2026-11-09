package com.varcal.cheermanager.Service.Notificaciones;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Eventos.Evento;
import com.varcal.cheermanager.Models.Horario_Asistencia.Horario;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.repository.Eventos.EventoRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.HorarioRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;

/** C7: calendarios descargables (.ics) de los horarios de un grupo y de los eventos del club. */
@Service
public class CalendarioService {

    private static final String[] DIA_ICS = { "MO", "TU", "WE", "TH", "FR", "SA", "SU" };

    private final GrupoEntrenamientoRepository grupos;
    private final HorarioRepository horarios;
    private final EventoRepository eventos;

    public CalendarioService(GrupoEntrenamientoRepository grupos, HorarioRepository horarios, EventoRepository eventos) {
        this.grupos = grupos;
        this.horarios = horarios;
        this.eventos = eventos;
    }

    protected LocalDate hoy() {
        return LocalDate.now();
    }

    protected LocalDateTime ahora() {
        return LocalDateTime.now(java.time.ZoneOffset.UTC);
    }

    public String nombreArchivoGrupo(Integer grupoId) {
        GrupoEntrenamiento g = grupos.findById(grupoId).orElseThrow(() -> new IllegalArgumentException("Grupo no encontrado"));
        return "entrenamientos-" + g.getNombre().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "") + ".ics";
    }

    /** Un evento semanal recurrente por cada bloque de horario del grupo. */
    public String calendarioGrupo(Integer grupoId) {
        GrupoEntrenamiento g = grupos.findById(grupoId).orElseThrow(() -> new IllegalArgumentException("Grupo no encontrado"));
        LocalDate hoy = hoy();
        List<String> vevents = new ArrayList<>();
        for (Horario h : horarios.findByGrupoEntrenamientoId(grupoId)) {
            if (h.getDia() == null || h.getDia().getId() == null || h.getDia().getId() < 1 || h.getDia().getId() > 7
                    || h.getHoraInicio() == null || h.getHoraFin() == null || !h.getHoraFin().isAfter(h.getHoraInicio())) {
                continue;
            }
            int dia = h.getDia().getId();
            LocalDate primera = hoy.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.of(dia)));
            vevents.add(IcsBuilder.eventoSemanal("grupo-" + grupoId + "-horario-" + h.getId() + "@cheermanager",
                    ahora(), primera, h.getHoraInicio(), h.getHoraFin(), DIA_ICS[dia - 1],
                    "Entrenamiento " + g.getNombre(), null));
        }
        return IcsBuilder.calendario("Entrenamientos " + g.getNombre(), vevents);
    }

    /** Eventos y competencias de hoy en adelante. */
    public String calendarioEventos() {
        LocalDate hoy = hoy();
        List<Evento> futuros = new ArrayList<>();
        for (Evento e : eventos.findAll()) {
            if (e.getFecha() != null && !e.getFecha().isBefore(hoy)) {
                futuros.add(e);
            }
        }
        futuros.sort(Comparator.comparing(Evento::getFecha));
        List<String> vevents = new ArrayList<>();
        for (Evento e : futuros) {
            vevents.add(IcsBuilder.eventoDiaCompleto("evento-" + e.getId() + "@cheermanager", ahora(), e.getFecha(),
                    e.getNombre(), e.getUbicacion(),
                    e.getTipoEvento() == null ? null : e.getTipoEvento().getEvento()));
        }
        return IcsBuilder.calendario("Eventos y competencias", vevents);
    }
}
