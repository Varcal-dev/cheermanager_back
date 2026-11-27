package com.varcal.cheermanager.Service.Importacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import com.varcal.cheermanager.DTO.Persona.DeportistaDTO;
import com.varcal.cheermanager.Models.Financiero.Convenio;
import com.varcal.cheermanager.Models.Org_dep.Nivel;
import com.varcal.cheermanager.Models.Personas.Acudiente;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.EstadoPersona;
import com.varcal.cheermanager.Models.Personas.Genero;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Clubes.SuscripcionService;
import com.varcal.cheermanager.Service.Importacion.ImportacionDeportistasService.Fila;
import com.varcal.cheermanager.Service.Importacion.ImportacionDeportistasService.Resultado;
import com.varcal.cheermanager.Service.Persona.AcudienteService;
import com.varcal.cheermanager.Service.Persona.PersonaService;
import com.varcal.cheermanager.repository.Financiero.ConvenioRepository;
import com.varcal.cheermanager.repository.Org_dep.NivelRepository;
import com.varcal.cheermanager.repository.Personas.AcudienteRepository;
import com.varcal.cheermanager.repository.Personas.EstadoPersonaRepository;
import com.varcal.cheermanager.repository.Personas.GeneroRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;
import com.varcal.cheermanager.tenant.TenantContext;

/**
 * TEST-01 (B): importacion masiva de deportistas desde CSV, con archivos y valores hostiles.
 *
 * Sin base de datos: PersonaService y los repositorios son simulados (Mockito). Lo que se fija es el contrato de la
 * importacion: validar NO guarda nada; importar guarda solo las filas correctas; una fila mala no tumba a las demas;
 * se respeta el cupo del plan; los valores demasiado largos se recortan; el documento duplicado (en el archivo o ya
 * en el sistema) se rechaza; y los problemas del archivo completo se informan sin guardar nada.
 *
 * El aislamiento por club de los catalogos (generos, niveles, convenios) y del documento unico lo hace Hibernate
 * (@TenantId) y no se prueba aqui. El parseo de celdas ya lo cubren ImportacionParseoTest y CsvUtilTest.
 */
class ImportacionDeportistasServiceTest {

    static final String CAB = "nombre;apellidos;numero_documento;fecha_nacimiento";
    static final String ADULTO = "1990-05-10";
    static final String MENOR = "2015-03-25";

    PersonaService personaService;
    AcudienteService acudienteService;
    SuscripcionService suscripcionService;
    PersonaRepository personaRepository;
    AcudienteRepository acudienteRepository;
    GeneroRepository generoRepository;
    EstadoPersonaRepository estadoRepository;
    NivelRepository nivelRepository;
    ConvenioRepository convenioRepository;

    ImportacionDeportistasService servicio;

    /** Estado de la simulacion que cada prueba puede ajustar. */
    Integer cupo = null;
    final Set<String> documentosExistentes = new HashSet<>();
    final Set<String> acudientesExistentes = new HashSet<>();
    final Set<String> documentosQueFallanAlGuardar = new HashSet<>();
    final List<DeportistaDTO> registrados = new ArrayList<>();

    static Genero genero(int id, String nombre) {
        Genero g = new Genero();
        g.setId(id);
        g.setGenero(nombre);
        return g;
    }

    static EstadoPersona estado(int id, String nombre) {
        EstadoPersona e = new EstadoPersona();
        e.setId(id);
        e.setEstado(nombre);
        return e;
    }

    @BeforeEach
    void preparar() {
        TenantContext.set(2);
        personaService = mock(PersonaService.class);
        acudienteService = mock(AcudienteService.class);
        suscripcionService = mock(SuscripcionService.class);
        personaRepository = mock(PersonaRepository.class);
        acudienteRepository = mock(AcudienteRepository.class);
        generoRepository = mock(GeneroRepository.class);
        estadoRepository = mock(EstadoPersonaRepository.class);
        nivelRepository = mock(NivelRepository.class);
        convenioRepository = mock(ConvenioRepository.class);

        when(generoRepository.findAll()).thenReturn(List.of(genero(1, "Femenino"), genero(2, "Masculino")));
        when(estadoRepository.findAll())
                .thenReturn(List.of(estado(1, "Activo"), estado(2, "Inactivo"), estado(3, "Retirado")));
        Nivel nivel = new Nivel();
        nivel.setId(10);
        nivel.setNombre("Nivel 1");
        when(nivelRepository.findAll()).thenReturn(List.of(nivel));
        Convenio convenio = new Convenio();
        convenio.setId(20);
        convenio.setNombreEmpresa("Empresa X");
        when(convenioRepository.findAll()).thenReturn(List.of(convenio));

        when(suscripcionService.cupoDisponible()).thenAnswer(i -> cupo);
        when(personaRepository.findByNumeroDocumento(any())).thenAnswer(
                i -> documentosExistentes.contains(i.getArgument(0)) ? Optional.of(new Persona()) : Optional.empty());
        when(acudienteRepository.findByNumeroDocumento(any())).thenAnswer(i -> {
            if (!acudientesExistentes.contains(i.getArgument(0))) {
                return Optional.empty();
            }
            Acudiente a = new Acudiente();
            a.setId(77);
            return Optional.of(a);
        });
        when(personaService.registrarDeportista(any(DeportistaDTO.class))).thenAnswer(i -> {
            DeportistaDTO dto = i.getArgument(0);
            if (documentosQueFallanAlGuardar.contains(dto.getNumeroDocumento())) {
                throw new RuntimeException("Error simulado al guardar");
            }
            registrados.add(dto);
            Deportista d = new Deportista();
            d.setId(100 + registrados.size());
            return d;
        });
        when(acudienteService.crear(any())).thenAnswer(i -> {
            Acudiente a = new Acudiente();
            a.setId(500);
            return a;
        });

        servicio = new ImportacionDeportistasService(personaService, acudienteService, suscripcionService,
                personaRepository, acudienteRepository, generoRepository, estadoRepository, nivelRepository,
                convenioRepository);
    }

    @AfterEach
    void limpiar() {
        TenantContext.clear();
    }

    // ------------------------------------------------------------------ utilidades

    static byte[] csv(String... lineas) {
        return String.join("\n", lineas).getBytes(StandardCharsets.UTF_8);
    }

    static Fila fila(Resultado r, int numeroFila) {
        return r.filas().stream().filter(f -> f.numero() == numeroFila).findFirst().orElseThrow();
    }

    static boolean algunoContiene(List<String> textos, String fragmento) {
        return textos.stream().anyMatch(t -> t.contains(fragmento));
    }

    // ------------------------------------------------------------------ validar vs importar

    @Test
    void validar_no_guarda_nada_y_informa_cada_fila() {
        Resultado r = servicio.validar(csv(CAB, "Ana;Perez;111;" + ADULTO, "Luis;Gomez;222;" + ADULTO));

        assertTrue(r.soloValidacion());
        assertEquals(2, r.total());
        assertEquals(2, r.validas());
        assertEquals(0, r.conErrores());
        assertEquals(0, r.importadas());
        assertEquals("OK", fila(r, 2).estado());
        assertEquals("OK", fila(r, 3).estado());
        verify(personaService, never()).registrarDeportista(any());
        verify(acudienteService, never()).crear(any());
    }

    @Test
    void importar_guarda_las_filas_correctas_con_los_campos_normalizados() {
        Resultado r = servicio.importar(csv(
                CAB + ";tipo_documento;genero;estado;nivel;convenio;altura;peso;telefono;direccion;contacto_emergencia",
                "  Ana   María ;  Pérez  ;1.006.123.456;" + ADULTO + ";cc;FEMENINO;ACTIVO;nivel 1;empresa x;150,4;45;3001234567;Calle 1;Mamá 3009999999"));

        assertFalse(r.soloValidacion());
        assertEquals(1, r.importadas());
        assertEquals("IMPORTADA", fila(r, 2).estado());
        assertEquals(1, registrados.size());
        DeportistaDTO d = registrados.get(0);
        assertEquals("Ana María", d.getNombre(), "los espacios sobrantes se limpian");
        assertEquals("Pérez", d.getApellidos());
        assertEquals("1006123456", d.getNumeroDocumento(), "el documento queda sin puntos");
        assertEquals("CC", d.getTipoDocumento());
        assertEquals(LocalDate.of(1990, 5, 10), d.getFechaNacimiento());
        assertEquals(1, d.getGeneroId());
        assertEquals(1, d.getEstadoId());
        assertEquals(10, d.getNivelActualId());
        assertEquals(20, d.getConvenioId());
        assertEquals(150, d.getAltura(), "150,4 se redondea a 150");
        assertEquals(45, d.getPeso());
        assertEquals("3001234567", d.getTelefono());
        assertEquals("Calle 1", d.getDireccion());
        assertEquals("Mamá 3009999999", d.getContactoEmergencia());
    }

    @Test
    void sin_columna_estado_se_importa_como_activo() {
        servicio.importar(csv(CAB, "Ana;Perez;111;" + ADULTO));
        assertEquals(1, registrados.get(0).getEstadoId());
    }

    @Test
    void una_fila_mala_no_tumba_a_las_demas() {
        Resultado r = servicio.importar(csv(CAB, "Ana;Perez;111;" + ADULTO, ";Gomez;222;" + ADULTO,
                "Eva;Ruiz;333;" + ADULTO));

        assertEquals(3, r.total());
        assertEquals(2, r.validas());
        assertEquals(1, r.conErrores());
        assertEquals(2, r.importadas());
        assertEquals("IMPORTADA", fila(r, 2).estado());
        assertEquals("ERROR", fila(r, 3).estado());
        assertEquals("IMPORTADA", fila(r, 4).estado());
        assertTrue(algunoContiene(fila(r, 3).errores(), "El nombre es obligatorio"));
        assertEquals(List.of("111", "333"), registrados.stream().map(DeportistaDTO::getNumeroDocumento).toList());
    }

    @Test
    void si_falla_el_guardado_de_una_fila_queda_como_error_y_no_se_crea_su_acudiente() {
        documentosQueFallanAlGuardar.add("222");
        Resultado r = servicio.importar(csv(
                CAB + ";acudiente_nombre;acudiente_apellidos;acudiente_telefono",
                "Ana;Perez;111;" + ADULTO + ";;;", "Luis;Gomez;222;" + ADULTO + ";Maria;Gomez;3001112222"));

        assertEquals(1, r.importadas());
        assertEquals(1, r.validas(), "la fila que fallo deja de contar como valida");
        assertEquals(1, r.conErrores());
        assertEquals("ERROR", fila(r, 3).estado());
        assertTrue(algunoContiene(fila(r, 3).errores(), "Error simulado al guardar"));
        verify(acudienteService, never()).crear(any());
        verify(acudienteService, never()).vincular(anyInt(), any());
    }

    // ------------------------------------------------------------------ documentos

    @Test
    void documento_repetido_en_el_archivo_se_rechaza_aunque_cambie_el_formato() {
        Resultado r = servicio.validar(csv(CAB, "Ana;Perez;1.006.123;" + ADULTO, "Luis;Gomez;1006123;" + ADULTO,
                "Eva;Ruiz;1 006 123;" + ADULTO));

        assertEquals("OK", fila(r, 2).estado());
        assertEquals("ERROR", fila(r, 3).estado());
        assertEquals("ERROR", fila(r, 4).estado());
        assertTrue(algunoContiene(fila(r, 3).errores(), "esta repetido en el archivo"));
    }

    @Test
    void documento_ya_registrado_en_el_sistema_se_rechaza() {
        documentosExistentes.add("111");
        Resultado r = servicio.importar(csv(CAB, "Ana;Perez;111;" + ADULTO, "Luis;Gomez;222;" + ADULTO));

        assertEquals("ERROR", fila(r, 2).estado());
        assertTrue(algunoContiene(fila(r, 2).errores(), "ya esta registrado en el sistema"));
        assertEquals(1, r.importadas());
        assertEquals(List.of("222"), registrados.stream().map(DeportistaDTO::getNumeroDocumento).toList());
    }

    @Test
    void los_cuatro_campos_obligatorios_vacios_dan_cuatro_errores() {
        // CsvUtil descarta las filas con TODAS las celdas vacias, asi que se deja un dato suelto en otra columna.
        Resultado r = servicio.validar(csv(CAB + ";telefono", ";;;;999"));

        List<String> errores = fila(r, 2).errores();
        assertTrue(algunoContiene(errores, "nombre es obligatorio"));
        assertTrue(algunoContiene(errores, "apellidos son obligatorios"));
        assertTrue(algunoContiene(errores, "documento es obligatorio"));
        assertTrue(algunoContiene(errores, "fecha de nacimiento es obligatoria"));
        assertEquals(4, errores.size());
    }

    // ------------------------------------------------------------------ valores hostiles

    @Test
    void fechas_invalidas_se_rechazan_y_los_formatos_validos_se_aceptan() {
        LocalDate manana = SuscripcionService.hoy().plusDays(1);
        Resultado r = servicio.validar(csv(CAB,
                "A;B;1;" + manana,           // 2: futura
                "A;B;2;1900-01-01",          // 3: demasiado antigua
                "A;B;3;31/02/2020",          // 4: dia inexistente
                "A;B;4;hola",                // 5: no es fecha
                "A;B;5;25/03/1990",          // 6: valida DD/MM/AAAA
                "A;B;6;25-03-1990"));        // 7: valida DD-MM-AAAA

        for (int n = 2; n <= 5; n++) {
            assertEquals("ERROR", fila(r, n).estado(), "fila " + n);
        }
        assertEquals("OK", fila(r, 6).estado());
        assertEquals("OK", fila(r, 7).estado());
        assertTrue(algunoContiene(fila(r, 2).errores(), "es futura"));
    }

    @Test
    void altura_y_peso_fuera_de_rango_o_no_numericos_se_rechazan() {
        Resultado r = servicio.validar(csv(CAB + ";altura;peso",
                "A;B;1;" + ADULTO + ";29;60",       // altura baja
                "A;B;2;" + ADULTO + ";261;60",      // altura alta
                "A;B;3;" + ADULTO + ";150;4",       // peso bajo
                "A;B;4;" + ADULTO + ";150;301",     // peso alto
                "A;B;5;" + ADULTO + ";abc;60",      // no es numero
                "A;B;6;" + ADULTO + ";150;60"));    // correcta

        for (int n = 2; n <= 6; n++) {
            assertEquals("ERROR", fila(r, n).estado(), "fila " + n);
        }
        assertEquals("OK", fila(r, 7).estado());
    }

    @Test
    void genero_nivel_y_convenio_inexistentes_se_rechazan_y_explican_las_opciones() {
        Resultado r = servicio.validar(csv(CAB + ";genero;nivel;convenio",
                "A;B;1;" + ADULTO + ";Otro;Nivel 1;Empresa X",
                "A;B;2;" + ADULTO + ";Femenino;Nivel 9;Empresa X",
                "A;B;3;" + ADULTO + ";Femenino;Nivel 1;Nada"));

        assertTrue(algunoContiene(fila(r, 2).errores(), "Genero 'Otro' no existe. Opciones: Femenino, Masculino"));
        assertTrue(algunoContiene(fila(r, 3).errores(), "Nivel 'Nivel 9' no existe"));
        assertTrue(algunoContiene(fila(r, 4).errores(), "Convenio 'Nada' no existe"));
        assertEquals(3, r.conErrores());
    }

    @Test
    void estado_retirado_o_inexistente_se_rechaza() {
        Resultado r = servicio.validar(csv(CAB + ";estado",
                "A;B;1;" + ADULTO + ";Retirado", "A;B;2;" + ADULTO + ";Suspendido", "A;B;3;" + ADULTO + ";Inactivo"));

        assertTrue(algunoContiene(fila(r, 2).errores(), "No se importa en estado Retirado"));
        assertTrue(algunoContiene(fila(r, 3).errores(), "Estado 'Suspendido' no existe"));
        assertEquals("OK", fila(r, 4).estado());
    }

    @Test
    void valores_larguisimos_se_recortan_y_el_texto_con_sql_viaja_como_dato() {
        String largo = "x".repeat(10_000);
        String sql = "Robert'); DROP TABLE personas;--";
        Resultado r = servicio.importar(csv(CAB + ";telefono;direccion;contacto_emergencia",
                largo + ";\"" + sql + "\";9;" + ADULTO + ";" + "9".repeat(200) + ";" + largo + ";" + largo));

        assertEquals(1, r.importadas());
        DeportistaDTO d = registrados.get(0);
        assertEquals(100, d.getNombre().length());
        assertEquals(sql.length(), d.getApellidos().length(), "el texto con SQL no se interpreta ni se altera");
        assertEquals(sql, d.getApellidos());
        assertEquals(30, d.getTelefono().length());
        assertEquals(200, d.getDireccion().length());
        assertEquals(200, d.getContactoEmergencia().length());
    }

    // ------------------------------------------------------------------ cupo del plan

    @Test
    void importar_respeta_el_cupo_y_rechaza_las_filas_que_lo_exceden() {
        cupo = 2;
        Resultado r = servicio.importar(csv(CAB, "A;B;1;" + ADULTO, "A;B;2;" + ADULTO, "A;B;3;" + ADULTO,
                "A;B;4;" + ADULTO));

        assertEquals(2, r.importadas());
        assertEquals(2, r.conErrores());
        assertEquals(2, r.cupoDisponible());
        assertEquals("IMPORTADA", fila(r, 2).estado());
        assertEquals("IMPORTADA", fila(r, 3).estado());
        assertEquals("ERROR", fila(r, 4).estado());
        assertTrue(algunoContiene(fila(r, 4).errores(), "Excede el cupo de deportistas del plan"));
        verify(personaService, times(2)).registrarDeportista(any());
    }

    @Test
    void validar_tambien_informa_el_cupo_sin_guardar() {
        cupo = 1;
        Resultado r = servicio.validar(csv(CAB, "A;B;1;" + ADULTO, "A;B;2;" + ADULTO));

        assertEquals(1, r.validas());
        assertEquals(1, r.conErrores());
        assertEquals("ERROR", fila(r, 3).estado());
        verify(personaService, never()).registrarDeportista(any());
    }

    @Test
    void cupo_cero_rechaza_todo_y_cupo_ilimitado_acepta_todo() {
        cupo = 0;
        Resultado sinCupo = servicio.importar(csv(CAB, "A;B;1;" + ADULTO, "A;B;2;" + ADULTO));
        assertEquals(0, sinCupo.importadas());
        assertEquals(2, sinCupo.conErrores());
        verify(personaService, never()).registrarDeportista(any());

        cupo = null;
        Resultado ilimitado = servicio.importar(csv(CAB, "A;B;1;" + ADULTO, "A;B;2;" + ADULTO, "A;B;3;" + ADULTO));
        assertEquals(3, ilimitado.importadas());
        assertNull(ilimitado.cupoDisponible());
    }

    @Test
    void el_cupo_no_se_gasta_en_filas_con_errores() {
        cupo = 2;
        Resultado r = servicio.importar(csv(CAB, ";B;1;" + ADULTO, "A;B;2;" + ADULTO, "A;B;3;" + ADULTO));

        assertEquals(2, r.importadas(), "la fila con error no consume cupo");
        assertEquals(1, r.conErrores());
    }

    // ------------------------------------------------------------------ acudiente

    @Test
    void crea_y_vincula_al_acudiente_como_principal() {
        Resultado r = servicio.importar(csv(
                CAB + ";acudiente_nombre;acudiente_apellidos;acudiente_tipo_documento;acudiente_documento;acudiente_telefono;acudiente_email;acudiente_parentesco",
                "Ana;Perez;111;" + MENOR + ";Maria;Perez;cc;55.123;3001112222;maria@correo.com;Madre"));

        assertEquals("IMPORTADA", fila(r, 2).estado());
        ArgumentCaptor<AcudienteService.AcudienteRequest> crear = ArgumentCaptor
                .forClass(AcudienteService.AcudienteRequest.class);
        verify(acudienteService).crear(crear.capture());
        assertEquals("Maria", crear.getValue().nombre());
        assertEquals("55123", crear.getValue().numeroDocumento());
        assertEquals("CC", crear.getValue().tipoDocumento());
        assertEquals("maria@correo.com", crear.getValue().email());

        ArgumentCaptor<AcudienteService.VinculoRequest> vinculo = ArgumentCaptor
                .forClass(AcudienteService.VinculoRequest.class);
        verify(acudienteService).vincular(org.mockito.ArgumentMatchers.eq(101), vinculo.capture());
        assertEquals(500, vinculo.getValue().acudienteId());
        assertEquals("Madre", vinculo.getValue().parentesco());
        assertEquals(Boolean.TRUE, vinculo.getValue().principal());
        assertEquals(Boolean.FALSE, vinculo.getValue().autorizaRecoger());
        assertFalse(algunoContiene(fila(r, 2).avisos(), "Menor de edad sin acudiente"));
    }

    @Test
    void si_el_acudiente_ya_existe_se_vincula_sin_duplicarlo() {
        acudientesExistentes.add("55123");
        Resultado r = servicio.importar(csv(
                CAB + ";acudiente_nombre;acudiente_apellidos;acudiente_documento;acudiente_telefono",
                "Ana;Perez;111;" + MENOR + ";Maria;Perez;55123;3001112222"));

        verify(acudienteService, never()).crear(any());
        ArgumentCaptor<AcudienteService.VinculoRequest> vinculo = ArgumentCaptor
                .forClass(AcudienteService.VinculoRequest.class);
        verify(acudienteService).vincular(org.mockito.ArgumentMatchers.eq(101), vinculo.capture());
        assertEquals(77, vinculo.getValue().acudienteId(), "se vincula al acudiente que ya existia");
        assertTrue(algunoContiene(fila(r, 2).avisos(), "ya existe: se vinculara al deportista"));
    }

    @Test
    void acudiente_incompleto_o_con_correo_invalido_es_error_de_la_fila() {
        Resultado r = servicio.validar(csv(
                CAB + ";acudiente_nombre;acudiente_apellidos;acudiente_telefono;acudiente_email",
                "A;B;1;" + ADULTO + ";Maria;;3001112222;",          // sin apellidos
                "A;B;2;" + ADULTO + ";Maria;Perez;;",               // sin telefono ni correo
                "A;B;3;" + ADULTO + ";Maria;Perez;;no-es-correo",   // correo invalido
                "A;B;4;" + ADULTO + ";Maria;Perez;;maria@correo.com"));

        assertTrue(algunoContiene(fila(r, 2).errores(), "nombre y apellidos son obligatorios"));
        assertTrue(algunoContiene(fila(r, 3).errores(), "al menos un telefono o un correo"));
        assertTrue(algunoContiene(fila(r, 4).errores(), "no parece valido"));
        assertEquals("OK", fila(r, 5).estado());
        assertTrue(algunoContiene(fila(r, 5).avisos(), "Acudiente sin documento"));
        verify(acudienteService, never()).crear(any());
    }

    // SEC-03: un fallo de base de datos no se le muestra al usuario (ni el SQL ni el nombre del constraint).
    private static final String SQL_BD = "could not execute statement [Duplicate entry '111' for key "
            + "'personas.uk_personas_documento'] [insert into personas (club_id, numero_documento) values (?, ?)]";

    @Test
    void un_fallo_de_base_de_datos_al_guardar_una_fila_no_muestra_el_sql() {
        doThrow(new DataIntegrityViolationException(SQL_BD)).when(personaService).registrarDeportista(any(DeportistaDTO.class));
        Resultado r = servicio.importar(csv(CAB, "Ana;Perez;111;" + ADULTO));

        assertEquals(0, r.importadas());
        assertEquals(1, r.conErrores());
        assertEquals("ERROR", fila(r, 2).estado());
        assertTrue(algunoContiene(fila(r, 2).errores(), "No se pudo guardar la fila"));
        assertFalse(algunoContiene(fila(r, 2).errores(), "insert into"));
        assertFalse(algunoContiene(fila(r, 2).errores(), "uk_personas_documento"));
    }

    @Test
    void un_fallo_de_base_de_datos_al_vincular_al_acudiente_no_muestra_el_sql_pero_si_el_aviso() {
        doThrow(new DataIntegrityViolationException(SQL_BD)).when(acudienteService).vincular(anyInt(), any());
        Resultado r = servicio.importar(csv(
                CAB + ";acudiente_nombre;acudiente_apellidos;acudiente_telefono;acudiente_parentesco",
                "Ana;Perez;111;" + MENOR + ";Maria;Perez;3001112222;Vecina"));

        assertEquals(1, r.importadas());
        assertEquals("IMPORTADA", fila(r, 2).estado());
        assertTrue(algunoContiene(fila(r, 2).avisos(),
                "no se pudo registrar al acudiente: intentalo desde la ficha del deportista"));
        assertFalse(algunoContiene(fila(r, 2).avisos(), "insert into"));
        assertFalse(algunoContiene(fila(r, 2).avisos(), "uk_personas_documento"));
    }

    @Test
    void si_falla_al_vincular_al_acudiente_el_deportista_se_queda_importado_con_un_aviso() {
        when(acudienteService.vincular(anyInt(), any())).thenThrow(new RuntimeException("Parentesco no valido"));
        Resultado r = servicio.importar(csv(
                CAB + ";acudiente_nombre;acudiente_apellidos;acudiente_telefono;acudiente_parentesco",
                "Ana;Perez;111;" + MENOR + ";Maria;Perez;3001112222;Vecina"));

        assertEquals(1, r.importadas());
        assertEquals(0, r.conErrores());
        assertEquals("IMPORTADA", fila(r, 2).estado());
        assertTrue(algunoContiene(fila(r, 2).avisos(), "no se pudo registrar al acudiente"));
        assertTrue(fila(r, 2).errores().isEmpty());
    }

    @Test
    void un_menor_sin_acudiente_avisa_y_un_adulto_no() {
        Resultado r = servicio.validar(csv(CAB, "A;B;1;" + MENOR, "A;B;2;" + ADULTO));

        assertEquals("OK", fila(r, 2).estado());
        assertTrue(algunoContiene(fila(r, 2).avisos(), "Menor de edad sin acudiente"));
        assertTrue(fila(r, 3).avisos().isEmpty());
    }

    // ------------------------------------------------------------------ el archivo completo

    @Test
    void archivo_vacio_o_nulo_se_rechaza_sin_guardar() {
        for (byte[] datos : new byte[][] { null, new byte[0] }) {
            Resultado r = servicio.importar(datos);
            assertEquals(List.of("El archivo esta vacio"), r.errores());
            assertEquals(0, r.total());
            assertEquals(0, r.importadas());
        }
        verify(personaService, never()).registrarDeportista(any());
    }

    @Test
    void archivo_de_mas_de_1_mb_se_rechaza_antes_de_leerlo() {
        byte[] enorme = new byte[ImportacionDeportistasService.MAX_BYTES + 1];
        java.util.Arrays.fill(enorme, (byte) 'a');

        Resultado r = servicio.importar(enorme);

        assertTrue(algunoContiene(r.errores(), "pesa mas de 1 MB"));
        verify(personaService, never()).registrarDeportista(any());
    }

    @Test
    void archivo_con_mas_de_mil_filas_se_rechaza_completo() {
        StringBuilder sb = new StringBuilder(CAB);
        for (int i = 0; i <= ImportacionDeportistasService.MAX_FILAS; i++) {
            sb.append("\nA;B;").append(i).append(';').append(ADULTO);
        }

        Resultado r = servicio.importar(sb.toString().getBytes(StandardCharsets.UTF_8));

        assertTrue(algunoContiene(r.errores(), "mas de 1000 filas"));
        assertEquals(0, r.importadas());
        verify(personaService, never()).registrarDeportista(any());
    }

    @Test
    void solo_encabezado_se_rechaza() {
        Resultado r = servicio.importar(csv(CAB));
        assertTrue(algunoContiene(r.errores(), "no tiene filas de datos"));
        verify(personaService, never()).registrarDeportista(any());
    }

    @Test
    void faltan_columnas_obligatorias_no_guarda_nada_y_las_nombra() {
        Resultado r = servicio.importar(csv("nombre;apellidos", "Ana;Perez"));

        assertTrue(r.errores().get(0).startsWith("Faltan columnas obligatorias:"));
        assertTrue(r.errores().get(0).contains("fecha_nacimiento"));
        assertTrue(r.errores().get(0).contains("numero_documento"));
        assertEquals(1, r.total());
        assertEquals(0, r.importadas());
        verify(personaService, never()).registrarDeportista(any());
    }

    @Test
    void columnas_desconocidas_o_repetidas_se_avisan_pero_el_archivo_se_procesa() {
        Resultado r = servicio.importar(csv(CAB + ";color_favorito;nombre", "Ana;Perez;111;" + ADULTO + ";rojo;Otro"));

        assertTrue(algunoContiene(r.errores(), "La columna 'color_favorito' no se reconoce y se ignorara"));
        assertTrue(algunoContiene(r.errores(), "La columna 'nombre' esta repetida; se usa la primera"));
        assertEquals(1, r.importadas());
        assertEquals("Ana", registrados.get(0).getNombre(), "se usa la primera columna repetida");
    }

    @Test
    void acepta_nombres_alternativos_de_columnas_bom_y_coma_como_separador() {
        String texto = "﻿Nombres,Apellido,Cédula,Fecha de nacimiento,Sexo\nAna,Perez,111," + ADULTO + ",Femenino";

        Resultado r = servicio.importar(texto.getBytes(StandardCharsets.UTF_8));

        assertEquals(1, r.importadas());
        assertEquals("111", registrados.get(0).getNumeroDocumento());
        assertEquals(1, registrados.get(0).getGeneroId());
    }
}
