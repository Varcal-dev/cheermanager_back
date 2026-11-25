package com.varcal.cheermanager.Service.Persona;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Persona.DeportistaDTO;
import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.EstadoPersona;
import com.varcal.cheermanager.Models.Personas.HistorialDeportistaEstado;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Clubes.SuscripcionService;
import com.varcal.cheermanager.Service.File.FileStorageService;
import com.varcal.cheermanager.Service.Historiales.HistorialNivelDeportistaService;
import com.varcal.cheermanager.repository.Auth.RolRepository;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Org_dep.InscripcionRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.repository.Personas.EstadoPersonaRepository;
import com.varcal.cheermanager.repository.Personas.HistorialDeportistaEstadoRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;
import com.varcal.cheermanager.tenant.ReferenciasClub;
import com.varcal.cheermanager.tenant.TenantContext;
import com.varcal.cheermanager.tenant.TenantLookupRepository;
import com.varcal.cheermanager.testsupport.Fakes;

/**
 * TEST-01 (C): el limite de deportistas del plan se exige en el alta y en la reactivacion de PersonaService.
 *
 * La logica del cupo en si (limite del plan, limite propio, paquetes, plan ilimitado) ya la cubre
 * SuscripcionPaquetesTest. Aqui se fija el CABLEADO, que es lo que se rompe en silencio al refactorizar:
 *  - registrarDeportista exige el cupo ANTES de guardar nada (ni persona, ni deportista, ni usuario a medias);
 *  - reactivarDeportista tambien lo exige, y solo despues de comprobar que el deportista esta Retirado;
 *  - no hay otra via para volver a activar a un retirado: modificarDeportista lo bloquea (en ambos sentidos);
 *  - ambos metodos son @Transactional, de modo que un fallo posterior revierte el alta completa.
 *
 * Sin base de datos: servicios y repositorios simulados con Mockito. El cupo contra MySQL real queda para MySqlRealIT.
 */
class AltaDeportistaCupoTest {

    static final String MENSAJE_CUPO = "Tu plan permite hasta 75 deportistas activos y el club ya tiene 75.";

    PersonaService servicio;
    SuscripcionService suscripcion;
    PersonaRepository personas;
    DeportistaRepository deportistas;
    UserRepository usuarios;
    RolRepository roles;
    EstadoPersonaRepository estados;
    HistorialDeportistaEstadoRepository historial;
    InscripcionRepository inscripciones;
    TenantLookupRepository lookup;

    EstadoPersona activo, inactivo, retirado;

    static EstadoPersona estado(int id, String nombre) {
        EstadoPersona e = new EstadoPersona();
        e.setId(id);
        e.setEstado(nombre);
        return e;
    }

    @BeforeEach
    void preparar() {
        TenantContext.set(2);
        activo = estado(1, "Activo");
        inactivo = estado(2, "Inactivo");
        retirado = estado(3, "Retirado");

        suscripcion = mock(SuscripcionService.class);
        personas = mock(PersonaRepository.class);
        deportistas = mock(DeportistaRepository.class);
        usuarios = mock(UserRepository.class);
        roles = mock(RolRepository.class);
        estados = mock(EstadoPersonaRepository.class);
        historial = mock(HistorialDeportistaEstadoRepository.class);
        inscripciones = mock(InscripcionRepository.class);
        lookup = mock(TenantLookupRepository.class);

        for (EstadoPersona e : List.of(activo, inactivo, retirado)) {
            when(estados.findById(e.getId())).thenReturn(Optional.of(e));
            when(estados.findByEstado(e.getEstado())).thenReturn(Optional.of(e));
        }
        Rol rolDeportista = new Rol();
        rolDeportista.setId(3);
        rolDeportista.setNombre("Deportista");
        when(roles.findByNombre("Deportista")).thenReturn(Optional.of(rolDeportista));
        when(roles.findById(3)).thenReturn(Optional.of(rolDeportista));

        when(personas.save(any(Persona.class))).thenAnswer(i -> i.getArgument(0));
        when(deportistas.save(any(Deportista.class))).thenAnswer(i -> {
            Deportista d = i.getArgument(0);
            if (d.getId() == null) {
                d.setId(100);
            }
            return d;
        });
        when(usuarios.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        servicio = new PersonaService();
        Fakes.inyectar(servicio, "suscripcionService", suscripcion);
        Fakes.inyectar(servicio, "personaRepository", personas);
        Fakes.inyectar(servicio, "deportistaRepository", deportistas);
        Fakes.inyectar(servicio, "userRepository", usuarios);
        Fakes.inyectar(servicio, "rolRepository", roles);
        Fakes.inyectar(servicio, "estadoPersonaRepository", estados);
        Fakes.inyectar(servicio, "historialDeportistaEstadoRepository", historial);
        Fakes.inyectar(servicio, "inscripcionRepository", inscripciones);
        Fakes.inyectar(servicio, "tenantLookup", lookup);
        Fakes.inyectar(servicio, "referencias", mock(ReferenciasClub.class));
        Fakes.inyectar(servicio, "historialNivelDeportistaService", mock(HistorialNivelDeportistaService.class));
        Fakes.inyectar(servicio, "fileStorageService", mock(FileStorageService.class));
    }

    @AfterEach
    void limpiar() {
        TenantContext.clear();
    }

    // ------------------------------------------------------------------ utilidades

    static DeportistaDTO dto(String documento, int estadoId) {
        DeportistaDTO d = new DeportistaDTO();
        d.setNombre("Ana");
        d.setApellidos("Perez");
        d.setNumeroDocumento(documento);
        d.setTipoDocumento("CC");
        d.setFechaNacimiento(LocalDate.of(2012, 3, 25));
        d.setGeneroId(1);
        d.setEstadoId(estadoId);
        return d;
    }

    Deportista deportistaConEstado(EstadoPersona estado) {
        Persona p = new Persona();
        p.setId(50);
        p.setNombre("Ana");
        p.setApellidos("Perez");
        Deportista d = new Deportista();
        d.setId(7);
        d.setPersona(p);
        d.setEstado(estado);
        when(deportistas.findById(7)).thenReturn(Optional.of(d));
        return d;
    }

    // ------------------------------------------------------------------ registrarDeportista

    @Test
    void con_el_cupo_lleno_el_alta_se_rechaza_sin_guardar_nada() {
        doThrow(new RuntimeException(MENSAJE_CUPO)).when(suscripcion).exigirCupoDeportista();

        RuntimeException e = assertThrows(RuntimeException.class, () -> servicio.registrarDeportista(dto("111", 1)));

        assertEquals(MENSAJE_CUPO, e.getMessage());
        // Ni siquiera se consulta el documento: el cupo es lo primero. Nada de persona/deportista/usuario a medias.
        verifyNoInteractions(personas, deportistas, usuarios, historial, roles);
    }

    @Test
    void con_cupo_disponible_se_exige_una_vez_antes_de_guardar_y_se_crean_persona_deportista_y_usuario() {
        Deportista creado = servicio.registrarDeportista(dto("111", 1));

        assertEquals(100, creado.getId());
        InOrder orden = inOrder(suscripcion, personas, deportistas, usuarios);
        orden.verify(suscripcion).exigirCupoDeportista();
        orden.verify(personas).save(any(Persona.class));
        orden.verify(deportistas).save(any(Deportista.class));
        orden.verify(usuarios).save(any(Usuario.class));
        verify(suscripcion, times(1)).exigirCupoDeportista();
        assertEquals(activo, creado.getEstado());
        verify(historial).save(any(HistorialDeportistaEstado.class)); // el alta queda en el historial de estados
    }

    @Test
    void el_usuario_del_deportista_nace_con_clave_aleatoria_y_obligado_a_cambiarla() {
        servicio.registrarDeportista(dto("111", 1));

        ArgumentCaptor<Usuario> captura = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captura.capture());
        Usuario u = captura.getValue();
        assertEquals("Deportista", u.getRol().getNombre());
        assertTrue(u.getActivo());
        assertTrue(u.getRequiereCambioPassword());
        assertTrue(u.getUsername().startsWith("ana.perez_"));
        assertTrue(u.getEmail().endsWith("@cheermanager.com"));
        assertFalse(BCrypt.checkpw("0000", u.getPasswordHash()), "ya no existe la clave 0000 para todos");
    }

    @Test
    void un_documento_ya_registrado_se_rechaza_despues_del_cupo_y_sin_guardar() {
        when(personas.findByNumeroDocumento("111")).thenReturn(Optional.of(new Persona()));

        RuntimeException e = assertThrows(RuntimeException.class, () -> servicio.registrarDeportista(dto("111", 1)));

        assertTrue(e.getMessage().contains("ya está registrado"));
        verify(suscripcion).exigirCupoDeportista();
        verify(personas, never()).save(any());
        verifyNoInteractions(deportistas, usuarios);
    }

    @Test
    void si_falla_la_creacion_del_usuario_el_error_sube_para_que_la_transaccion_revierta() {
        when(lookup.usernameExisteEnAlgunClub(any())).thenReturn(true);

        RuntimeException e = assertThrows(RuntimeException.class, () -> servicio.registrarDeportista(dto("111", 1)));

        assertEquals("El nombre de usuario ya está en uso", e.getMessage());
        verify(usuarios, never()).save(any());
    }

    @Test
    void alta_y_reactivacion_son_transaccionales() throws Exception {
        assertTrue(PersonaService.class.getMethod("registrarDeportista", DeportistaDTO.class)
                .isAnnotationPresent(Transactional.class), "sin @Transactional un fallo dejaria persona/deportista huerfanos");
        assertTrue(PersonaService.class.getMethod("reactivarDeportista", Integer.class, String.class)
                .isAnnotationPresent(Transactional.class));
        assertTrue(PersonaService.class.getMethod("modificarDeportista", Integer.class, DeportistaDTO.class)
                .isAnnotationPresent(Transactional.class));
    }

    // ------------------------------------------------------------------ reactivarDeportista

    @Test
    void con_el_cupo_lleno_no_se_reactiva_a_un_retirado() {
        Deportista d = deportistaConEstado(retirado);
        doThrow(new RuntimeException(MENSAJE_CUPO)).when(suscripcion).exigirCupoDeportista();

        RuntimeException e = assertThrows(RuntimeException.class, () -> servicio.reactivarDeportista(7, "volvio"));

        assertEquals(MENSAJE_CUPO, e.getMessage());
        assertEquals(retirado, d.getEstado(), "sigue Retirado");
        verify(deportistas, never()).save(any());
        verifyNoInteractions(historial);
        verify(usuarios, never()).reactivarPorPersona(anyInt(), anyInt());
    }

    @Test
    void con_cupo_disponible_reactiva_deja_historial_y_reactiva_su_usuario() {
        Deportista d = deportistaConEstado(retirado);
        when(usuarios.reactivarPorPersona(50, 2)).thenReturn(1);

        PersonaService.ReactivacionResultado r = servicio.reactivarDeportista(7, "  error de digitacion ");

        verify(suscripcion, times(1)).exigirCupoDeportista();
        assertEquals(activo, d.getEstado());
        assertEquals("Activo", r.estado());
        assertTrue(r.usuarioReactivado());
        assertTrue(r.requiereMatricula(), "no se reabre la inscripcion: hace falta una matricula nueva");
        verify(deportistas).save(d);
        ArgumentCaptor<HistorialDeportistaEstado> h = ArgumentCaptor.forClass(HistorialDeportistaEstado.class);
        verify(historial).save(h.capture());
        assertEquals("Deportista reactivado: error de digitacion", h.getValue().getMotivoCambio());
        assertEquals(retirado, h.getValue().getEstadoAnterior());
        assertEquals(activo, h.getValue().getEstadoNuevo());
    }

    @Test
    void el_motivo_de_la_reactivacion_se_recorta_a_255_y_vacio_usa_el_texto_por_defecto() {
        deportistaConEstado(retirado);
        servicio.reactivarDeportista(7, "x".repeat(400));
        ArgumentCaptor<HistorialDeportistaEstado> largo = ArgumentCaptor.forClass(HistorialDeportistaEstado.class);
        verify(historial).save(largo.capture());
        assertEquals(255, largo.getValue().getMotivoCambio().length());

        Deportista d2 = deportistaConEstado(retirado);
        servicio.reactivarDeportista(7, "   ");
        ArgumentCaptor<HistorialDeportistaEstado> vacio = ArgumentCaptor.forClass(HistorialDeportistaEstado.class);
        verify(historial, times(2)).save(vacio.capture());
        assertEquals("Deportista reactivado", vacio.getAllValues().get(1).getMotivoCambio());
        assertEquals(activo, d2.getEstado());
    }

    @Test
    void solo_se_reactiva_a_un_retirado_y_se_comprueba_antes_de_mirar_el_cupo() {
        Deportista d = deportistaConEstado(activo);

        RuntimeException e = assertThrows(RuntimeException.class, () -> servicio.reactivarDeportista(7, null));

        assertEquals("Solo se puede reactivar a un deportista en estado Retirado", e.getMessage());
        verifyNoInteractions(suscripcion, historial);
        verify(deportistas, never()).save(any());
        assertEquals(activo, d.getEstado());
    }

    @Test
    void reactivar_un_deportista_inexistente_falla_sin_tocar_nada() {
        when(deportistas.findById(999)).thenReturn(Optional.empty());

        RuntimeException e = assertThrows(RuntimeException.class, () -> servicio.reactivarDeportista(999, null));

        assertEquals("Deportista no encontrado", e.getMessage());
        verifyNoInteractions(suscripcion, historial);
    }

    // ------------------------------------------------------------------ no hay otra via para activar a un retirado

    @Test
    void modificar_no_permite_volver_a_activar_a_un_retirado_saltandose_el_cupo() {
        Deportista d = deportistaConEstado(retirado);

        RuntimeException e = assertThrows(RuntimeException.class, () -> servicio.modificarDeportista(7, dto(null, 1)));

        assertTrue(e.getMessage().contains("usa la opcion Reactivar"));
        assertEquals(retirado, d.getEstado());
        verify(deportistas, never()).save(any());
        verifyNoInteractions(historial);
    }

    @Test
    void modificar_tampoco_permite_retirar_a_mano() {
        Deportista d = deportistaConEstado(activo);

        RuntimeException e = assertThrows(RuntimeException.class, () -> servicio.modificarDeportista(7, dto(null, 3)));

        assertTrue(e.getMessage().contains("usa la opcion Retirar"));
        assertEquals(activo, d.getEstado());
        verify(deportistas, never()).save(any());
    }

    @Test
    void cambiar_entre_estados_no_retirados_no_consume_cupo_porque_ambos_ya_cuentan_como_activos() {
        Deportista d = deportistaConEstado(activo);

        servicio.modificarDeportista(7, dto(null, 2));

        assertEquals(inactivo, d.getEstado());
        verify(deportistas).save(d);
        verify(historial).save(any(HistorialDeportistaEstado.class));
        verifyNoInteractions(suscripcion);
        assertNotEquals(retirado, d.getEstado());
    }
}
