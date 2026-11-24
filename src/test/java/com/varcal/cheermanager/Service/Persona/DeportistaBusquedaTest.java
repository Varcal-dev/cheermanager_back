package com.varcal.cheermanager.Service.Persona;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.varcal.cheermanager.DTO.Persona.DeportistaBusquedaDTO;
import com.varcal.cheermanager.DTO.Persona.DeportistaOpcionDTO;
import com.varcal.cheermanager.Models.Org_dep.Nivel;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.repository.Org_dep.NivelRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

/** PERF-01 (5b): busqueda ligera y lista minima de deportistas. Sin base de datos. */
class DeportistaBusquedaTest {

    private final DeportistaRepository deportistas = mock(DeportistaRepository.class);
    private final NivelRepository niveles = mock(NivelRepository.class);
    private final PersonaService service = new PersonaService();

    DeportistaBusquedaTest() {
        ReflectionTestUtils.setField(service, "deportistaRepository", deportistas);
        ReflectionTestUtils.setField(service, "nivelRepository", niveles);
    }

    private static Deportista deportista(int id, int personaId, String nombre, String apellidos, Integer nivelId) {
        Persona p = new Persona();
        p.setId(personaId);
        p.setNombre(nombre);
        p.setApellidos(apellidos);
        Deportista d = new Deportista();
        d.setId(id);
        d.setPersona(p);
        d.setNivelActualId(nivelId);
        return d;
    }

    @Test
    void conMenosDeDosLetrasNoConsultaNada() {
        assertTrue(service.buscarDeportistas(null, 6).isEmpty());
        assertTrue(service.buscarDeportistas("  a ", 6).isEmpty());
        verify(deportistas, never()).buscarPorNombre(anyString(), any(Pageable.class));
    }

    @Test
    void elTopeDeResultadosEsDe20YElMinimoDe1() {
        when(deportistas.buscarPorNombre(anyString(), any(Pageable.class))).thenReturn(List.of());

        service.buscarDeportistas("ana", 500);
        service.buscarDeportistas("ana", -3);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(deportistas, org.mockito.Mockito.times(2)).buscarPorNombre(eq("ana"), pageable.capture());
        assertEquals(20, pageable.getAllValues().get(0).getPageSize());
        assertEquals(1, pageable.getAllValues().get(1).getPageSize());
    }

    @Test
    void devuelveElNombreDelNivelOSinNivel() {
        Nivel n = new Nivel();
        n.setId(3);
        n.setNombre("Nivel 3");
        when(niveles.findAll()).thenReturn(List.of(n));
        when(deportistas.buscarPorNombre(anyString(), any(Pageable.class))).thenReturn(List.of(
                deportista(10, 100, "Ana", "Pérez", 3),
                deportista(11, 101, "Ana", "Gómez", null),
                deportista(12, 102, "Ana", "Ruiz", 99)));   // nivel que ya no existe en el catalogo

        List<DeportistaBusquedaDTO> r = service.buscarDeportistas("  ana ", 6);

        assertEquals(3, r.size());
        assertEquals(new DeportistaBusquedaDTO(10, 100, "Ana", "Pérez", "Nivel 3"), r.get(0));
        assertNull(r.get(1).nivelNombre());
        assertNull(r.get(2).nivelNombre());
        verify(deportistas).buscarPorNombre(eq("ana"), any(Pageable.class));   // se recorta el texto
    }

    @Test
    void opcionesDevuelvenSoloLoMinimo() {
        when(deportistas.listarParaOpciones()).thenReturn(List.of(deportista(10, 100, "Ana", "Pérez", 3)));

        List<DeportistaOpcionDTO> r = service.listarDeportistasOpciones();

        assertEquals(List.of(new DeportistaOpcionDTO(10, 100, "Ana", "Pérez")), r);
    }
}
