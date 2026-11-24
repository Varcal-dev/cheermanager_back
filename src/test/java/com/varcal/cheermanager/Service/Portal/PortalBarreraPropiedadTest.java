package com.varcal.cheermanager.Service.Portal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Org_dep.SolicitudInscripcion;
import com.varcal.cheermanager.Models.Personas.Acudiente;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Carnet.CarnetService;
import com.varcal.cheermanager.Service.Financiero.FacturaService;
import com.varcal.cheermanager.Service.Financiero.PagoService;
import com.varcal.cheermanager.Service.Org_dep.SolicitudInscripcionService;
import com.varcal.cheermanager.Service.Perfil.PerfilCompetenciaSaludService;
import com.varcal.cheermanager.Service.Perfil.PerfilDeportistaService;
import com.varcal.cheermanager.Service.Persona.PersonaService;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Evaluaciones.EvaluacionRepository;
import com.varcal.cheermanager.repository.Evaluaciones.ObjetivoRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.EvaluacionRutinaRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.AsistenciaRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.HorarioEntrenamientoRepository;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;
import com.varcal.cheermanager.repository.Org_dep.InscripcionRepository;
import com.varcal.cheermanager.repository.Personas.AcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaAcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.testsupport.Fakes;

/**
 * TEST-01 (portal): la barrera de propiedad PortalService.propio().
 *
 * El permiso acceder_portal NO basta para ver datos ajenos: cada consulta debe comprobar que el deportista sea del
 * usuario (el mismo, o un hijo a cargo de un acudiente). Aqui se fija que:
 *  - un acudiente/deportista NO accede a un deportista de otra familia en NINGUNO de los metodos del portal;
 *  - el error es identico al de un deportista que no existe (no revela que el id es real);
 *  - cuando se rechaza, no se llega a consultar ningun dato (ni facturas, ni salud, ni carnet, ni foto);
 *  - lo propio sigue funcionando (un acudiente con dos hijos ve a ambos).
 *
 * Sin base de datos: servicios y repositorios simulados con Mockito. El aislamiento entre CLUBES lo hace Hibernate
 * (@TenantId) y se prueba en AislamientoEntreClubesTest y MySqlRealIT.
 */
class PortalBarreraPropiedadTest {

    // Familia A: usuario 10 = acudiente (id 5) de los deportistas 1 y 2.
    // Familia B: usuario 11 = acudiente (id 6) del deportista 3.
    // Usuario 20 = deportista adulto (deportista 4, persona 104) con su propia cuenta.
    static final int USUARIO_ACUDIENTE_A = 10;
    static final int USUARIO_ACUDIENTE_B = 11;
    static final int USUARIO_DEPORTISTA_ADULTO = 20;
    static final int DEPORTISTA_HIJO_1 = 1;
    static final int DEPORTISTA_HIJO_2 = 2;
    static final int DEPORTISTA_AJENO = 3;
    static final int DEPORTISTA_ADULTO = 4;
    static final int DEPORTISTA_INEXISTENTE = 999;
    static final String NO_ENCONTRADO = "Deportista no encontrado";

    UserRepository usuarios;
    DeportistaRepository deportistas;
    AcudienteRepository acudientes;
    DeportistaAcudienteRepository vinculos;
    FacturaService facturaService;
    PagoService pagoService;
    AsistenciaRepository asistencias;
    EvaluacionRepository evaluaciones;
    ObjetivoRepository objetivos;
    EvaluacionRutinaRepository rutinas;
    DeportistaPerteneceGrupoRepository grupos;
    InscripcionRepository inscripciones;
    SolicitudInscripcionService solicitudService;
    CarnetService carnetService;
    HorarioEntrenamientoRepository horarios;
    PersonaService personaService;
    PerfilDeportistaService perfilService;
    PerfilCompetenciaSaludService competenciaSaludService;

    PortalService portal;

    Deportista hijo1, hijo2, ajeno, adulto;

    static Deportista deportista(int id, int personaId, String nombre) {
        Persona p = new Persona();
        p.setId(personaId);
        p.setNombre(nombre);
        p.setApellidos("Prueba");
        Deportista d = new Deportista();
        d.setId(id);
        d.setPersona(p);
        return d;
    }

    static Acudiente acudiente(int id) {
        Acudiente a = new Acudiente();
        a.setId(id);
        a.setNombre("Acudiente" + id);
        a.setApellidos("Prueba");
        return a;
    }

    static Usuario usuario(int id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername("u" + id);
        return u;
    }

    /** Simula la sesion que deja JwtTokenFilter: el userId viaja en Authentication.details. */
    static void iniciarSesion(int usuarioId) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("u" + usuarioId, null,
                List.of());
        auth.setDetails(Map.of("userId", (long) usuarioId));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @BeforeEach
    void preparar() {
        hijo1 = deportista(DEPORTISTA_HIJO_1, 101, "Hijo1");
        hijo2 = deportista(DEPORTISTA_HIJO_2, 102, "Hijo2");
        ajeno = deportista(DEPORTISTA_AJENO, 103, "Ajeno");
        adulto = deportista(DEPORTISTA_ADULTO, 104, "Adulto");

        Usuario uAcudienteA = usuario(USUARIO_ACUDIENTE_A);
        Usuario uAcudienteB = usuario(USUARIO_ACUDIENTE_B);
        Usuario uAdulto = usuario(USUARIO_DEPORTISTA_ADULTO);
        uAdulto.setPersona(adulto.getPersona());

        usuarios = mock(UserRepository.class);
        when(usuarios.findById(10L)).thenReturn(Optional.of(uAcudienteA));
        when(usuarios.findById(11L)).thenReturn(Optional.of(uAcudienteB));
        when(usuarios.findById(20L)).thenReturn(Optional.of(uAdulto));

        deportistas = mock(DeportistaRepository.class);
        when(deportistas.findByPersonaId(104)).thenReturn(Optional.of(adulto));

        Acudiente acA = acudiente(5), acB = acudiente(6);
        acudientes = mock(AcudienteRepository.class);
        when(acudientes.findByUsuarioId(USUARIO_ACUDIENTE_A)).thenReturn(Optional.of(acA));
        when(acudientes.findByUsuarioId(USUARIO_ACUDIENTE_B)).thenReturn(Optional.of(acB));

        vinculos = mock(DeportistaAcudienteRepository.class);
        when(vinculos.deportistasDeAcudiente(5)).thenReturn(List.of(hijo1, hijo2));
        when(vinculos.deportistasDeAcudiente(6)).thenReturn(List.of(ajeno));

        facturaService = mock(FacturaService.class);
        pagoService = mock(PagoService.class);
        asistencias = mock(AsistenciaRepository.class);
        evaluaciones = mock(EvaluacionRepository.class);
        objetivos = mock(ObjetivoRepository.class);
        rutinas = mock(EvaluacionRutinaRepository.class);
        grupos = mock(DeportistaPerteneceGrupoRepository.class);
        inscripciones = mock(InscripcionRepository.class);
        solicitudService = mock(SolicitudInscripcionService.class);
        carnetService = mock(CarnetService.class);
        horarios = mock(HorarioEntrenamientoRepository.class);
        personaService = mock(PersonaService.class);
        perfilService = mock(PerfilDeportistaService.class);
        competenciaSaludService = mock(PerfilCompetenciaSaludService.class);

        portal = new PortalService(usuarios, deportistas, acudientes, vinculos, facturaService, pagoService,
                asistencias, evaluaciones, objetivos, rutinas, grupos, inscripciones, solicitudService);
        Fakes.inyectar(portal, "carnetService", carnetService);
        Fakes.inyectar(portal, "horarioEntrenamientoRepository", horarios);
        Fakes.inyectar(portal, "personaService", personaService);
        Fakes.inyectar(portal, "perfilService", perfilService);
        Fakes.inyectar(portal, "perfilCompetenciaSaludService", competenciaSaludService);
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    /** Todas las consultas del portal que reciben el id de un deportista. */
    interface Consulta {
        void ejecutar(PortalService p, Integer deportistaId) throws Exception;
    }

    static Map<String, Consulta> consultas() {
        Map<String, Consulta> m = new LinkedHashMap<>();
        m.put("facturas", (p, id) -> p.facturas(id));
        m.put("pagos", (p, id) -> p.pagos(id));
        m.put("asistencias", (p, id) -> p.asistencias(id, null, null));
        m.put("evaluaciones", (p, id) -> p.evaluaciones(id));
        m.put("objetivos", (p, id) -> p.objetivos(id));
        m.put("rutinas", (p, id) -> p.rutinas(id));
        m.put("resumen", (p, id) -> p.resumen(id));
        m.put("competenciaPerfil", (p, id) -> p.competenciaPerfil(id));
        m.put("saludPerfil", (p, id) -> p.saludPerfil(id));
        m.put("carnet", (p, id) -> p.carnet(id));
        m.put("foto", (p, id) -> p.foto(id));
        m.put("horarios", (p, id) -> p.horarios(id));
        m.put("solicitudes", (p, id) -> p.solicitudes(id));
        m.put("solicitar", (p, id) -> p.solicitar(id, 1, "obs"));
        return m;
    }

    private String mensajeAlConsultar(Consulta c, int deportistaId) {
        RuntimeException e = assertThrows(RuntimeException.class, () -> c.ejecutar(portal, deportistaId));
        return e.getMessage();
    }

    @Test
    void un_acudiente_no_accede_a_un_deportista_de_otra_familia_en_ningun_metodo() {
        iniciarSesion(USUARIO_ACUDIENTE_A);
        for (Map.Entry<String, Consulta> c : consultas().entrySet()) {
            assertEquals(NO_ENCONTRADO, mensajeAlConsultar(c.getValue(), DEPORTISTA_AJENO),
                    "el metodo '" + c.getKey() + "' debe rechazar a un deportista ajeno");
        }
    }

    @Test
    void el_rechazo_no_consulta_ningun_dato() {
        iniciarSesion(USUARIO_ACUDIENTE_A);
        for (Consulta c : consultas().values()) {
            mensajeAlConsultar(c, DEPORTISTA_AJENO);
        }
        verifyNoInteractions(facturaService, pagoService, asistencias, evaluaciones, objetivos, rutinas, grupos,
                inscripciones, carnetService, horarios, personaService, perfilService, competenciaSaludService);
        verify(solicitudService, never()).listarDeDeportista(anyInt());
        verify(solicitudService, never()).crear(anyInt(), anyInt(), any(), anyInt());
    }

    @Test
    void un_deportista_ajeno_responde_igual_que_uno_inexistente() {
        iniciarSesion(USUARIO_ACUDIENTE_A);
        for (Map.Entry<String, Consulta> c : consultas().entrySet()) {
            assertEquals(mensajeAlConsultar(c.getValue(), DEPORTISTA_INEXISTENTE),
                    mensajeAlConsultar(c.getValue(), DEPORTISTA_AJENO),
                    "el metodo '" + c.getKey() + "' no debe distinguir entre ajeno e inexistente");
        }
    }

    @Test
    void la_otra_familia_tampoco_ve_a_los_hijos_de_la_primera() {
        iniciarSesion(USUARIO_ACUDIENTE_B);
        for (Map.Entry<String, Consulta> c : consultas().entrySet()) {
            assertEquals(NO_ENCONTRADO, mensajeAlConsultar(c.getValue(), DEPORTISTA_HIJO_1), c.getKey());
            assertEquals(NO_ENCONTRADO, mensajeAlConsultar(c.getValue(), DEPORTISTA_HIJO_2), c.getKey());
        }
        verifyNoInteractions(facturaService, pagoService, carnetService, personaService, competenciaSaludService);
    }

    @Test
    void un_acudiente_con_dos_hijos_ve_a_ambos_y_solo_sus_datos() {
        iniciarSesion(USUARIO_ACUDIENTE_A);
        portal.facturas(DEPORTISTA_HIJO_1);
        portal.facturas(DEPORTISTA_HIJO_2);
        verify(facturaService).listarPorPersona(101);
        verify(facturaService).listarPorPersona(102);
        verify(facturaService, never()).listarPorPersona(103);

        portal.pagos(DEPORTISTA_HIJO_2);
        verify(pagoService).listarPorPersona(102);

        portal.carnet(DEPORTISTA_HIJO_1);
        verify(carnetService).carnet(DEPORTISTA_HIJO_1);
        verify(carnetService, never()).carnet(DEPORTISTA_AJENO);
    }

    @Test
    void un_deportista_adulto_solo_ve_lo_suyo() throws Exception {
        iniciarSesion(USUARIO_DEPORTISTA_ADULTO);
        portal.facturas(DEPORTISTA_ADULTO);
        verify(facturaService).listarPorPersona(104);

        for (int ajenoId : new int[] { DEPORTISTA_HIJO_1, DEPORTISTA_HIJO_2, DEPORTISTA_AJENO }) {
            assertEquals(NO_ENCONTRADO, mensajeAlConsultar((p, id) -> p.facturas(id), ajenoId));
            assertEquals(NO_ENCONTRADO, mensajeAlConsultar((p, id) -> p.saludPerfil(id), ajenoId));
            assertEquals(NO_ENCONTRADO, mensajeAlConsultar((p, id) -> p.foto(id), ajenoId));
        }
        verify(facturaService, never()).listarPorPersona(101);
        verify(competenciaSaludService, never()).salud(anyInt(), anyBoolean());
        verifyNoInteractions(personaService);
    }

    @Test
    void perfil_lista_solo_los_deportistas_a_cargo_del_usuario() {
        iniciarSesion(USUARIO_ACUDIENTE_A);
        PortalService.MiPerfil perfil = portal.perfil();
        assertEquals("ACUDIENTE", perfil.tipo());
        List<Integer> ids = perfil.deportistas().stream().map(PortalService.DeportistaResumen::id).toList();
        assertEquals(List.of(DEPORTISTA_HIJO_1, DEPORTISTA_HIJO_2), ids);
        assertTrue(!ids.contains(DEPORTISTA_AJENO));

        iniciarSesion(USUARIO_DEPORTISTA_ADULTO);
        PortalService.MiPerfil propio = portal.perfil();
        assertEquals("DEPORTISTA", propio.tipo());
        assertEquals(List.of(DEPORTISTA_ADULTO),
                propio.deportistas().stream().map(PortalService.DeportistaResumen::id).toList());
    }

    @Test
    void el_resumen_marca_si_quien_consulta_es_el_acudiente() {
        iniciarSesion(USUARIO_ACUDIENTE_A);
        portal.resumen(DEPORTISTA_HIJO_1);
        verify(perfilService).paraPortal(hijo1, true);

        iniciarSesion(USUARIO_DEPORTISTA_ADULTO);
        portal.resumen(DEPORTISTA_ADULTO);
        verify(perfilService).paraPortal(adulto, false);
    }

    @Test
    void solicitar_inscripcion_solo_para_un_deportista_propio_y_con_el_usuario_de_la_sesion() {
        iniciarSesion(USUARIO_ACUDIENTE_A);
        portal.solicitar(DEPORTISTA_HIJO_2, 7, "quiere entrar");
        verify(solicitudService).crear(DEPORTISTA_HIJO_2, 7, "quiere entrar", USUARIO_ACUDIENTE_A);
    }

    @Test
    void cancelar_una_solicitud_exige_que_sea_de_un_deportista_propio() {
        SolicitudInscripcion deLaFamilia = new SolicitudInscripcion();
        deLaFamilia.setDeportista(hijo1);
        SolicitudInscripcion deOtraFamilia = new SolicitudInscripcion();
        deOtraFamilia.setDeportista(ajeno);
        when(solicitudService.obtener(50)).thenReturn(deLaFamilia);
        when(solicitudService.obtener(60)).thenReturn(deOtraFamilia);

        iniciarSesion(USUARIO_ACUDIENTE_A);
        assertEquals(NO_ENCONTRADO, assertThrows(RuntimeException.class, () -> portal.cancelarSolicitud(60)).getMessage());
        verify(solicitudService, never()).cancelar(60);

        portal.cancelarSolicitud(50);
        verify(solicitudService).cancelar(50);
    }

    @Test
    void sin_sesion_o_con_un_usuario_que_ya_no_existe_no_se_devuelve_nada() {
        SecurityContextHolder.clearContext();
        assertThrows(RuntimeException.class, () -> portal.facturas(DEPORTISTA_HIJO_1));

        iniciarSesion(999); // token valido pero el usuario fue eliminado
        RuntimeException e = assertThrows(RuntimeException.class, () -> portal.facturas(DEPORTISTA_HIJO_1));
        assertEquals("Usuario no encontrado", e.getMessage());
        verifyNoInteractions(facturaService, pagoService);
    }

    @Test
    void un_usuario_sin_ficha_ni_acudiente_no_ve_a_nadie() {
        Usuario soloCuenta = usuario(30);
        when(usuarios.findById(30L)).thenReturn(Optional.of(soloCuenta));

        iniciarSesion(30);
        for (Map.Entry<String, Consulta> c : consultas().entrySet()) {
            assertEquals(NO_ENCONTRADO, mensajeAlConsultar(c.getValue(), DEPORTISTA_HIJO_1), c.getKey());
        }
        assertTrue(portal.perfil().deportistas().isEmpty());
    }
}
