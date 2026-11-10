package com.varcal.cheermanager.Service.Horario_Asistencia;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Horario_Asistencia.AsignacionEspacio;
import com.varcal.cheermanager.Models.Horario_Asistencia.Espacio;
import com.varcal.cheermanager.Models.Horario_Asistencia.Espacio.Tipo;
import com.varcal.cheermanager.Models.Horario_Asistencia.Horario;
import com.varcal.cheermanager.Models.Horario_Asistencia.HorarioEntrenamiento;
import com.varcal.cheermanager.Models.Horario_Asistencia.Sede;
import com.varcal.cheermanager.Models.Org_dep.DeportistaPerteneceGrupo;
import com.varcal.cheermanager.Models.Org_dep.DiaSemana;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.repository.Horario_Asistencia.AsignacionEspacioRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.EspacioRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.HorarioEntrenamientoRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.SedeRepository;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** C11: sedes, espacios y choques de horario. */
class SedeServiceTest {

    final List<Sede> sedes = new ArrayList<>();
    final List<Espacio> espacios = new ArrayList<>();
    final List<AsignacionEspacio> asignaciones = new ArrayList<>();
    final List<HorarioEntrenamiento> horarios = new ArrayList<>();
    final List<DeportistaPerteneceGrupo> miembros = new ArrayList<>();

    final SedeService service = new SedeService(
            Fakes.proxy(SedeRepository.class, new Repo()
                    .on("save", a -> { Sede s = (Sede) a[0]; if (s.getId() == null) { s.setId(sedes.size() + 1); sedes.add(s); } return s; })
                    .on("findById", a -> sedes.stream().filter(s -> s.getId().equals(a[0])).findFirst())
                    .on("findAllByOrderByNombreAsc", a -> new ArrayList<>(sedes))
                    .on("existsByNombreIgnoreCase", a -> sedes.stream().anyMatch(s -> s.getNombre().equalsIgnoreCase((String) a[0])))),
            Fakes.proxy(EspacioRepository.class, new Repo()
                    .on("save", a -> { Espacio e = (Espacio) a[0]; if (e.getId() == null) { e.setId(espacios.size() + 1); espacios.add(e); } return e; })
                    .on("findById", a -> espacios.stream().filter(e -> e.getId().equals(a[0])).findFirst())
                    .on("findBySedeIdOrderByNombreAsc", a -> espacios.stream().filter(e -> e.getSedeId().equals(a[0])).toList())
                    .on("existsBySedeIdAndNombreIgnoreCase", a -> espacios.stream().anyMatch(e -> e.getSedeId().equals(a[0]) && e.getNombre().equalsIgnoreCase((String) a[1])))),
            Fakes.proxy(AsignacionEspacioRepository.class, new Repo()
                    .on("save", a -> { AsignacionEspacio x = (AsignacionEspacio) a[0]; if (x.getId() == null) { x.setId(asignaciones.size() + 1); asignaciones.add(x); } return x; })
                    .on("findAll", a -> asignaciones)
                    .on("findByHorarioEntrenamientoId", a -> asignaciones.stream().filter(x -> x.getHorarioEntrenamientoId().equals(a[0])).findFirst())
                    .on("findByEspacioId", a -> asignaciones.stream().filter(x -> x.getEspacioId().equals(a[0])).toList())
                    .on("deleteById", a -> { asignaciones.removeIf(x -> x.getId().equals(a[0])); return null; })),
            Fakes.proxy(HorarioEntrenamientoRepository.class, new Repo()
                    .on("findAll", a -> horarios)
                    .on("findById", a -> horarios.stream().filter(h -> h.getId().equals(a[0])).findFirst())),
            Fakes.proxy(DeportistaPerteneceGrupoRepository.class, new Repo()
                    .on("findByGrupoIdAndFechaFinIsNull", a -> miembros.stream().filter(m -> m.getGrupo().getId().equals(a[0])).toList())));

    void reiniciar() {
        sedes.clear(); espacios.clear(); asignaciones.clear(); horarios.clear(); miembros.clear();
        // Horarios de grupo: 1 = Elite lunes 17-19, 2 = Cubs lunes 18-20 (se cruza), 3 = Wild martes 17-19, 4 = Black lunes 19-21 (pegado, no cruza con 1)
        he(1, 10, "Elite", 1, 17, 19);
        he(2, 11, "Cubs", 1, 18, 20);
        he(3, 12, "Wild", 2, 17, 19);
        he(4, 13, "Black", 1, 19, 21);
    }

    void he(int id, int grupoId, String nombre, int dia, int ini, int fin) {
        DiaSemana d = new DiaSemana(); d.setId(dia); d.setDia(dia == 1 ? "Lunes" : "Martes");
        Horario h = new Horario(); h.setDia(d); h.setHoraInicio(LocalTime.of(ini, 0)); h.setHoraFin(LocalTime.of(fin, 0));
        GrupoEntrenamiento g = new GrupoEntrenamiento(); g.setId(grupoId); g.setNombre(nombre);
        HorarioEntrenamiento x = new HorarioEntrenamiento(); x.setId(id); x.setHorario(h); x.setGrupoEntrenamiento(g);
        horarios.add(x);
    }

    Espacio espacio(String nombre, Integer cap, int simultaneos) {
        if (sedes.isEmpty()) service.crearSede(new SedeService.SedeSolicitud("Sede Centro", "Cra 1", "Florencia", null, null));
        return service.crearEspacio(1, new SedeService.EspacioSolicitud(nombre, Tipo.COLCHONETA, cap, simultaneos, null));
    }

    @Test
    void sedesYEspaciosConReglasDeNombre() {
        reiniciar();
        service.crearSede(new SedeService.SedeSolicitud("Sede Centro", "Cra 1 #2-3", "Florencia", null, null));
        assertThrows(IllegalStateException.class, () -> service.crearSede(new SedeService.SedeSolicitud("sede centro", null, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> service.crearSede(new SedeService.SedeSolicitud("x", null, null, null, null)));
        service.crearEspacio(1, new SedeService.EspacioSolicitud("Colchoneta A", Tipo.COLCHONETA, 30, null, null));
        assertThrows(IllegalStateException.class, () -> service.crearEspacio(1, new SedeService.EspacioSolicitud("colchoneta a", Tipo.SALON, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> service.crearEspacio(1, new SedeService.EspacioSolicitud("Salon", null, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> service.crearEspacio(1, new SedeService.EspacioSolicitud("Salon", Tipo.SALON, 0, null, null)));
        assertThrows(IllegalArgumentException.class, () -> service.crearEspacio(99, new SedeService.EspacioSolicitud("Salon", Tipo.SALON, null, null, null)));
        assertEquals(1, service.listar().get(0).espacios().size());
    }

    @Test
    void choqueEnElMismoEspacioSeBloquea() {
        reiniciar();
        Espacio a = espacio("Colchoneta A", null, 1);
        service.asignar(1, a.getId());
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> service.asignar(2, a.getId())); // se cruza lunes 18-19
        assertTrue(e.getMessage().contains("Elite"));
        service.asignar(3, a.getId()); // otro dia, si
        service.asignar(4, a.getId()); // pegado 19-21, si
        assertEquals(3, asignaciones.size());
    }

    @Test
    void espacioConVariosGruposSimultaneos() {
        reiniciar();
        Espacio grande = espacio("Gimnasio", null, 2);
        service.asignar(1, grande.getId());
        service.asignar(2, grande.getId()); // 2 grupos a la vez, permitido
        // un tercer grupo que se cruce con los dos ya no cabe: Black (19-21) no se cruza, asi que creamos otro lunes 18-19
        he(5, 14, "Extra", 1, 18, 19);
        assertThrows(IllegalStateException.class, () -> service.asignar(5, grande.getId()));
        service.asignar(4, grande.getId()); // 19-21 no se cruza con 17-19 ni 18-20 (18-20 termina a las 20 y Black empieza a las 19: cruza solo con Cubs)
    }

    @Test
    void reasignarUnHorarioNoChocaConSigoMismo() {
        reiniciar();
        Espacio a = espacio("Colchoneta A", null, 1);
        service.asignar(1, a.getId());
        service.asignar(1, a.getId()); // misma asignacion otra vez: no es choque
        assertEquals(1, asignaciones.size());
        Espacio b = service.crearEspacio(1, new SedeService.EspacioSolicitud("Salon B", Tipo.SALON, null, null, null));
        service.asignar(1, b.getId()); // cambia de espacio
        assertEquals(1, asignaciones.size());
        assertEquals(b.getId(), asignaciones.get(0).getEspacioId());
    }

    @Test
    void avisaSiElGrupoNoCabeSinBloquear() {
        reiniciar();
        Espacio chico = espacio("Salon chico", 2, 1);
        for (int i = 0; i < 3; i++) { DeportistaPerteneceGrupo m = new DeportistaPerteneceGrupo(); m.setGrupo(horarios.get(0).getGrupoEntrenamiento()); miembros.add(m); }
        SedeService.Asignacion r = service.asignar(1, chico.getId());
        assertEquals(1, r.advertencias().size());
        assertTrue(r.advertencias().get(0).contains("3 deportistas"));
    }

    @Test
    void desactivarYBajarLimiteExigenReasignar() {
        reiniciar();
        Espacio a = espacio("Gimnasio", null, 2);
        service.asignar(1, a.getId());
        service.asignar(2, a.getId());
        assertThrows(IllegalArgumentException.class, () -> service.actualizarEspacio(a.getId(), new SedeService.EspacioSolicitud(null, null, null, 1, null)));
        assertThrows(IllegalStateException.class, () -> service.actualizarEspacio(a.getId(), new SedeService.EspacioSolicitud(null, null, null, null, false)));
        assertThrows(IllegalStateException.class, () -> service.actualizarSede(1, new SedeService.SedeSolicitud(null, null, null, null, false)));
        service.quitarAsignacion(1);
        service.quitarAsignacion(2);
        assertThrows(IllegalArgumentException.class, () -> service.quitarAsignacion(2));
        service.actualizarEspacio(a.getId(), new SedeService.EspacioSolicitud(null, null, null, null, false));
        service.actualizarSede(1, new SedeService.SedeSolicitud(null, null, null, null, false));
        assertThrows(IllegalStateException.class, () -> service.asignar(1, a.getId())); // espacio/sede desactivados
    }

    @Test
    void tableroMuestraOcupacionYHorariosSinEspacio() {
        reiniciar();
        Espacio a = espacio("Colchoneta A", null, 1);
        service.asignar(1, a.getId());
        service.asignar(3, a.getId());
        SedeService.Tablero t = service.tablero();
        assertEquals(1, t.espacios().size());
        assertEquals(2, t.espacios().get(0).usos().size());
        assertEquals(2, t.sinEspacio().size()); // Cubs y Black sin espacio
        assertEquals("Cubs", t.sinEspacio().get(0).grupo());
    }
}
