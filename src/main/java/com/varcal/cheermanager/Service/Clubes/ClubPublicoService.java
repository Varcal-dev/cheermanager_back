package com.varcal.cheermanager.Service.Clubes;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Clubes.Club;
import com.varcal.cheermanager.Models.Clubes.EstadoSuscripcion;
import com.varcal.cheermanager.Models.Horario_Asistencia.HorarioEntrenamiento;
import com.varcal.cheermanager.Models.Horario_Asistencia.Sede;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.Tenant.TenantContext;
import com.varcal.cheermanager.repository.Clubes.ClubRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.HorarioEntrenamientoRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.SedeRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;

/**
 * Informacion publica de cada club para la pagina "Unete": nombre, sedes, grupos y horarios.
 * Nunca expone personas, cupos ni cifras del club.
 */
@Service
public class ClubPublicoService {

    public record HorarioPublico(String dia, String inicio, String fin) {
    }

    public record GrupoPublico(String nombre, String tipo, String nivel, String division, List<HorarioPublico> horarios) {
    }

    public record SedePublica(String nombre, String direccion, String ciudad) {
    }

    /** Fila del directorio. */
    public record ResumenClub(String nombre, String slug, int totalGrupos, List<String> niveles, List<String> ciudades) {
    }

    /** Ficha completa de un club. */
    public record FichaClub(String nombre, String slug, List<SedePublica> sedes, List<GrupoPublico> grupos) {
    }

    @Autowired
    private ClubRepository clubes;
    @Autowired
    private GrupoEntrenamientoRepository grupos;
    @Autowired
    private HorarioEntrenamientoRepository horarios;
    @Autowired
    private SedeRepository sedes;

    private static boolean visible(Club c) {
        return c != null && Boolean.TRUE.equals(c.getActivo()) && !Integer.valueOf(Club.PLATAFORMA_ID).equals(c.getId())
                && c.getEstadoSuscripcion() != EstadoSuscripcion.CANCELADA;
    }

    public List<ResumenClub> directorio() {
        List<ResumenClub> lista = new ArrayList<>();
        for (Club c : clubes.findAll()) {
            if (!visible(c)) {
                continue;
            }
            FichaClub f = TenantContext.<FichaClub>runAs(c.getId(), () -> ficha(c));
            Set<String> niveles = new LinkedHashSet<>();
            f.grupos().forEach(g -> {
                if (g.nivel() != null) {
                    niveles.add(g.nivel());
                }
            });
            Set<String> ciudades = new LinkedHashSet<>();
            f.sedes().forEach(s -> {
                if (s.ciudad() != null && !s.ciudad().isBlank()) {
                    ciudades.add(s.ciudad());
                }
            });
            lista.add(new ResumenClub(c.getNombre(), c.getSlug(), f.grupos().size(), new ArrayList<>(niveles), new ArrayList<>(ciudades)));
        }
        lista.sort(Comparator.comparing(r -> r.nombre().toLowerCase()));
        return lista;
    }

    public Optional<FichaClub> porSlug(String slug) {
        if (slug == null) {
            return Optional.empty();
        }
        Club c = clubes.findBySlug(slug.toLowerCase()).orElse(null);
        if (!visible(c)) {
            return Optional.empty();
        }
        return Optional.of(TenantContext.<FichaClub>runAs(c.getId(), () -> ficha(c)));
    }

    /** Debe llamarse dentro del contexto del club. */
    private FichaClub ficha(Club c) {
        Map<Integer, List<HorarioPublico>> porGrupo = new TreeMap<>();
        for (HorarioEntrenamiento he : horarios.findAll()) {
            if (he.getGrupoEntrenamiento() == null || he.getHorario() == null || he.getHorario().getDia() == null) {
                continue;
            }
            porGrupo.computeIfAbsent(he.getGrupoEntrenamiento().getId(), k -> new ArrayList<>())
                    .add(new HorarioPublico(he.getHorario().getDia().getDia(), hora(he.getHorario().getHoraInicio()),
                            hora(he.getHorario().getHoraFin())));
        }
        List<GrupoPublico> gs = new ArrayList<>();
        for (GrupoEntrenamiento g : grupos.findAll()) {
            var cat = g.getCategoriaNivel();
            gs.add(new GrupoPublico(g.getNombre(), g.getTipoGrupo() != null ? g.getTipoGrupo().getNombre() : null,
                    cat != null && cat.getNivel() != null ? cat.getNivel().getNombre() : null,
                    cat != null && cat.getDivision() != null ? cat.getDivision().getNombre() : null,
                    porGrupo.getOrDefault(g.getId(), List.of())));
        }
        gs.sort(Comparator.comparing(GrupoPublico::nombre, String.CASE_INSENSITIVE_ORDER));
        List<SedePublica> ss = new ArrayList<>();
        for (Sede s : sedes.findAllByOrderByNombreAsc()) {
            if (s.isActiva()) {
                ss.add(new SedePublica(s.getNombre(), s.getDireccion(), s.getCiudad()));
            }
        }
        return new FichaClub(c.getNombre(), c.getSlug(), ss, gs);
    }

    private static String hora(LocalTime t) {
        return t == null ? "" : String.format("%02d:%02d", t.getHour(), t.getMinute());
    }
}
