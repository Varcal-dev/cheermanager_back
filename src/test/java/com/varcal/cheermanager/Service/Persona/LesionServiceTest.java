package com.varcal.cheermanager.Service.Persona;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.EstadoPersona;
import com.varcal.cheermanager.Models.Personas.Lesion;
import com.varcal.cheermanager.Models.Personas.Lesion.Estado;
import com.varcal.cheermanager.Models.Personas.Lesion.Gravedad;
import com.varcal.cheermanager.Models.Personas.Lesion.Restriccion;
import com.varcal.cheermanager.Models.Personas.Lesion.Zona;
import com.varcal.cheermanager.Models.Personas.LesionSeguimiento;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Persona.LesionService.Seguimiento;
import com.varcal.cheermanager.Service.Persona.LesionService.Solicitud;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.repository.Personas.LesionRepository;
import com.varcal.cheermanager.repository.Personas.LesionSeguimientoRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** C3: lesiones y retorno al juego. */
class LesionServiceTest {

    final LocalDate hoy = LocalDate.of(2026, 11, 9);
    final List<Lesion> lesiones = new ArrayList<>();
    final List<LesionSeguimiento> notas = new ArrayList<>();
    final List<Deportista> deportistas = new ArrayList<>();
    boolean sinConsentimiento = false;

    final LesionService service = new LesionService(
            Fakes.proxy(LesionRepository.class, new Repo()
                    .on("save", a -> { Lesion l = (Lesion) a[0]; if (l.getId() == null) { l.setId(lesiones.size() + 1); lesiones.add(l); } return l; })
                    .on("findById", a -> lesiones.stream().filter(l -> l.getId().equals(a[0])).findFirst())
                    .on("findAllByOrderByFechaLesionDescIdDesc", a -> new ArrayList<>(lesiones))
                    .on("findByEstadoNotOrderByFechaLesionDescIdDesc", a -> lesiones.stream().filter(l -> l.getEstado() != a[0]).toList())
                    .on("findByDeportistaIdOrderByFechaLesionDescIdDesc", a -> lesiones.stream().filter(l -> l.getDeportistaId().equals(a[0])).toList())
                    .on("existsByDeportistaIdAndZonaAndEstadoNot", a -> lesiones.stream()
                            .anyMatch(l -> l.getDeportistaId().equals(a[0]) && l.getZona() == a[1] && l.getEstado() != a[2]))),
            Fakes.proxy(LesionSeguimientoRepository.class, new Repo()
                    .on("save", a -> { LesionSeguimiento n = (LesionSeguimiento) a[0]; n.setId(notas.size() + 1); notas.add(n); return n; })
                    .on("findByLesionIdOrderByIdAsc", a -> notas.stream().filter(n -> n.getLesionId().equals(a[0])).toList())),
            Fakes.proxy(DeportistaRepository.class, new Repo()
                    .on("findAll", a -> deportistas)
                    .on("findById", a -> deportistas.stream().filter(d -> d.getId().equals(a[0])).findFirst())),
            null) {
        @Override protected LocalDate hoy() { return hoy; }
        @Override protected LocalDateTime ahora() { return LocalDateTime.of(2026, 11, 9, 10, 0); }
        @Override protected void exigirSalud(Integer personaId) {
            if (sinConsentimiento) throw new RuntimeException("No hay un consentimiento vigente para tratar datos de salud");
        }
    };

    void reiniciar() {
        lesiones.clear(); notas.clear(); deportistas.clear(); sinConsentimiento = false;
        for (int id = 1; id <= 2; id++) {
            Persona p = new Persona(); p.setId(100 + id); p.setNombre("Dep" + id); p.setApellidos("Ape");
            EstadoPersona e = new EstadoPersona(); e.setEstado("Activo");
            Deportista d = new Deportista(); d.setId(id); d.setPersona(p); d.setEstado(e);
            deportistas.add(d);
        }
    }

    Solicitud leve() {
        return new Solicitud(Zona.TOBILLO_PIE, "Esguince al aterrizar", Gravedad.LEVE, Restriccion.MODIFICADO, hoy.minusDays(2), hoy.plusDays(10));
    }

    Seguimiento nota(String t, Estado e, Restriccion r) { return new Seguimiento(t, e, r, null, null, null); }

    @Test
    void registraYVeEnListaVigente() {
        reiniciar();
        LesionService.Item i = service.registrar(1, leve(), 7);
        assertEquals(Estado.ACTIVA, i.estado());
        assertEquals(2, i.dias());
        assertFalse(i.retornoAtrasado());
        LesionService.Resumen r = service.listar(true);
        assertEquals(1, r.items().size());
        assertEquals(1, r.modificado());
        assertEquals(0, r.sinEntrenar());
    }

    @Test
    void validacionesDeRegistro() {
        reiniciar();
        assertThrows(IllegalArgumentException.class, () -> service.registrar(99, leve(), 7));
        assertThrows(IllegalArgumentException.class, () -> service.registrar(1,
                new Solicitud(Zona.RODILLA, "abc", Gravedad.LEVE, Restriccion.MODIFICADO, hoy, null), 7)); // descripcion corta
        assertThrows(IllegalArgumentException.class, () -> service.registrar(1,
                new Solicitud(Zona.RODILLA, "Golpe en rodilla", Gravedad.LEVE, Restriccion.MODIFICADO, hoy.plusDays(1), null), 7)); // futura
        assertThrows(IllegalArgumentException.class, () -> service.registrar(1,
                new Solicitud(Zona.RODILLA, "Golpe en rodilla", Gravedad.LEVE, Restriccion.SIN_RESTRICCION, hoy, null), 7));
        assertThrows(IllegalArgumentException.class, () -> service.registrar(1,
                new Solicitud(Zona.RODILLA, "Ruptura de ligamento", Gravedad.GRAVE, Restriccion.MODIFICADO, hoy, null), 7)); // grave => sin entrenar
        assertTrue(lesiones.isEmpty());
    }

    @Test
    void noDuplicaLesionAbiertaEnLaMismaZona() {
        reiniciar();
        service.registrar(1, leve(), 7);
        assertThrows(IllegalStateException.class, () -> service.registrar(1, leve(), 7));
        service.registrar(2, leve(), 7); // otro deportista si
        assertEquals(2, lesiones.size());
    }

    @Test
    void exigeConsentimientoDeSaludCuandoEstaActivo() {
        reiniciar();
        sinConsentimiento = true;
        assertThrows(RuntimeException.class, () -> service.registrar(1, leve(), 7));
        assertTrue(lesiones.isEmpty());
    }

    @Test
    void retornoProgresivoHastaElAlta() {
        reiniciar();
        service.registrar(1, leve(), 7);
        LesionService.Item r = service.seguimiento(1, nota("Ya apoya el pie", Estado.EN_RETORNO, null), 7);
        assertEquals(Estado.EN_RETORNO, r.estado());
        assertEquals(Restriccion.MODIFICADO, r.restriccion());
        assertThrows(IllegalArgumentException.class, () -> service.seguimiento(1, nota("Sin restriccion aun", Estado.EN_RETORNO, Restriccion.SIN_RESTRICCION), 7));
        LesionService.Item alta = service.seguimiento(1, nota("Recuperado", Estado.ALTA, null), 7);
        assertEquals(Estado.ALTA, alta.estado());
        assertEquals(Restriccion.SIN_RESTRICCION, alta.restriccion());
        assertEquals(hoy, alta.fechaAlta());
        assertEquals(2, notas.size());
        assertThrows(IllegalStateException.class, () -> service.seguimiento(1, nota("Otra nota", null, null), 7));
        assertEquals(0, service.listar(true).items().size());
        assertEquals(1, service.listar(false).items().size());
    }

    @Test
    void graveSoloSeCierraConAvalMedico() {
        reiniciar();
        service.registrar(1, new Solicitud(Zona.RODILLA, "Ruptura de ligamento", Gravedad.GRAVE, Restriccion.SIN_ENTRENAR, hoy.minusDays(30), null), 7);
        assertThrows(IllegalArgumentException.class, () -> service.seguimiento(1, nota("Dice que ya esta bien", Estado.ALTA, null), 7));
        assertEquals(Estado.ACTIVA, lesiones.get(0).getEstado());
        LesionService.Item ok = service.seguimiento(1, new Seguimiento("Alta del ortopedista", Estado.ALTA, null, null, true, "Dr. Perez, 5/11"), 7);
        assertEquals(Estado.ALTA, ok.estado());
        assertTrue(ok.avalMedico());
        assertEquals("Dr. Perez, 5/11", ok.avalDetalle());
    }

    @Test
    void retornoAtrasadoYRestriccionVigente() {
        reiniciar();
        service.registrar(1, new Solicitud(Zona.HOMBRO, "Subluxacion", Gravedad.MODERADA, Restriccion.SIN_ENTRENAR, hoy.minusDays(20), hoy.minusDays(1)), 7);
        service.registrar(1, new Solicitud(Zona.MUSCULAR, "Sobrecarga", Gravedad.LEVE, Restriccion.MODIFICADO, hoy.minusDays(3), null), 7);
        LesionService.Resumen r = service.listar(true);
        assertEquals(1, r.retornosAtrasados());
        assertEquals(1, r.sinEntrenar());
        assertEquals(Restriccion.SIN_ENTRENAR, service.restriccionVigente(1));
        assertEquals(Restriccion.SIN_RESTRICCION, service.restriccionVigente(2));
        service.seguimiento(1, nota("Mejora", Estado.EN_RETORNO, null), 7);
        assertEquals(Restriccion.MODIFICADO, service.restriccionVigente(1));
        assertEquals(2, service.candidatos().size());
    }
}
