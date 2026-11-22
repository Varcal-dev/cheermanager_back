package com.varcal.cheermanager.DTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.DTO.Ath.UsuarioDTO;
import com.varcal.cheermanager.DTO.Comunes.Crear;
import com.varcal.cheermanager.DTO.Persona.ActualizarPerfilDTO;
import com.varcal.cheermanager.DTO.Persona.DeportistaDTO;
import com.varcal.cheermanager.DTO.Persona.EntrenadorDTO;
import com.varcal.cheermanager.DTO.Persona.HistorialMedicoDTO;
import com.varcal.cheermanager.DTO.Persona.VincularDeportistaUsuarioDTO;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.groups.Default;

/**
 * SEC-02 fase 2: reglas de personas, deportistas, entrenadores y usuarios.
 * "alta" = grupo Crear (@Validated(Crear.class)); "edicion" = grupo Default (@Valid).
 */
class ValidacionPersonasUsuariosTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void iniciar() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrar() {
        fabrica.close();
    }

    private static Set<String> alta(Object o) {
        return nombres(validador.validate(o, Crear.class));
    }

    private static Set<String> edicion(Object o) {
        return nombres(validador.validate(o, Default.class));
    }

    private static Set<String> nombres(Set<? extends jakarta.validation.ConstraintViolation<?>> v) {
        Set<String> r = new TreeSet<>();
        v.forEach(x -> r.add(x.getPropertyPath().toString()));
        return r;
    }

    // ---------- Deportista ----------

    private static DeportistaDTO deportista() {
        DeportistaDTO d = new DeportistaDTO();
        d.setNombre("Ana");
        d.setApellidos("Pérez");
        d.setNumeroDocumento("1006123456");
        d.setTipoDocumento("TI");
        d.setGeneroId(1);
        d.setEstadoId(1);
        d.setFechaNacimiento(LocalDate.of(2012, 5, 1));
        return d;
    }

    @Test
    void deportista_valido_en_alta_y_edicion() {
        assertTrue(alta(deportista()).isEmpty());
        assertTrue(edicion(deportista()).isEmpty());
    }

    @Test
    void deportista_alta_exige_documento_y_tipo_pero_edicion_no() {
        DeportistaDTO d = deportista();
        d.setNumeroDocumento(" ");
        d.setTipoDocumento(null);
        assertEquals(Set.of("numeroDocumento", "tipoDocumento"), alta(d));
        assertTrue(edicion(d).isEmpty());
    }

    @Test
    void deportista_exige_nombre_apellidos_genero_y_estado_siempre() {
        assertEquals(Set.of("nombre", "apellidos", "generoId", "estadoId"), edicion(new DeportistaDTO()));
        assertTrue(alta(new DeportistaDTO()).containsAll(Set.of("nombre", "apellidos", "generoId", "estadoId")));
    }

    @Test
    void deportista_fecha_futura_altura_y_peso_fuera_de_rango_y_textos_largos() {
        DeportistaDTO d = deportista();
        d.setFechaNacimiento(LocalDate.now().plusDays(1));
        d.setAltura(301);
        d.setPeso(-1);
        d.setDireccion("x".repeat(256));
        d.setTelefono("1".repeat(256));
        d.setContactoEmergencia("c".repeat(256));
        assertEquals(Set.of("fechaNacimiento", "altura", "peso", "direccion", "telefono", "contactoEmergencia"),
                edicion(d));
    }

    @Test
    void deportista_acepta_altura_y_peso_cero_o_nulos() {
        DeportistaDTO d = deportista();
        d.setAltura(0);
        d.setPeso(null); // el front manda 0 o nada cuando el campo esta vacio
        assertTrue(edicion(d).isEmpty());
        d.setAltura(300);
        d.setPeso(500);
        assertTrue(edicion(d).isEmpty());
        d.setFechaNacimiento(LocalDate.now()); // hoy es valido
        assertTrue(edicion(d).isEmpty());
    }

    // ---------- Entrenador ----------

    private static EntrenadorDTO entrenador() {
        EntrenadorDTO d = new EntrenadorDTO();
        d.setNombre("Luis");
        d.setApellidos("Gómez");
        d.setGeneroId(2);
        d.setEstadoId(1);
        d.setFechaContratacion(LocalDate.of(2026, 1, 1));
        return d;
    }

    @Test
    void entrenador_alta_exige_datos_basicos() {
        assertTrue(alta(entrenador()).isEmpty());
        assertEquals(Set.of("nombre", "apellidos", "generoId", "fechaContratacion", "estadoId"),
                alta(new EntrenadorDTO()));
    }

    @Test
    void entrenador_patch_parcial_sigue_valido_pero_respeta_formato() {
        EntrenadorDTO parcial = new EntrenadorDTO();
        parcial.setTelefono("3001234567"); // PATCH solo con telefono
        assertTrue(edicion(parcial).isEmpty());
        parcial.setEspecializacion("e".repeat(256));
        parcial.setFechaNacimiento(LocalDate.now().plusYears(1));
        assertEquals(Set.of("especializacion", "fechaNacimiento"), edicion(parcial));
    }

    // ---------- Usuario ----------

    private static UsuarioDTO usuario() {
        UsuarioDTO d = new UsuarioDTO();
        d.setUsername("ana.perez");
        d.setEmail("ana@club.com");
        d.setPassword("clave-segura-123");
        return d;
    }

    @Test
    void usuario_alta_valido() {
        assertTrue(alta(usuario()).isEmpty());
    }

    @Test
    void usuario_alta_exige_clave_de_8_a_72() {
        UsuarioDTO d = usuario();
        d.setPassword(null);
        assertEquals(Set.of("password"), alta(d));
        d.setPassword("1234567");
        assertEquals(Set.of("password"), alta(d));
        d.setPassword("x".repeat(73));
        assertEquals(Set.of("password"), alta(d));
        d.setPassword("x".repeat(72));
        assertTrue(alta(d).isEmpty());
    }

    @Test
    void usuario_edicion_clave_opcional_y_correo_heredado_no_bloquea() {
        UsuarioDTO d = usuario();
        d.setPassword(null); // sin cambio de clave
        assertTrue(edicion(d).isEmpty());
        d.setPassword("");
        assertTrue(edicion(d).isEmpty());
        d.setEmail("correo-heredado-sin-formato"); // acciones rapidas reenvian el correo actual
        assertTrue(edicion(d).isEmpty());
        assertEquals(Set.of("email"), alta(d)); // pero al crear si se exige formato
    }

    @Test
    void usuario_exige_username_y_email() {
        UsuarioDTO d = new UsuarioDTO();
        assertEquals(Set.of("username", "email"), edicion(d));
        assertTrue(alta(d).containsAll(Set.of("username", "email", "password")));
    }

    // ---------- Perfil propio ----------

    @Test
    void perfil_propio_valida_formato_pero_no_obliga_nada() {
        ActualizarPerfilDTO d = new ActualizarPerfilDTO();
        assertTrue(edicion(d).isEmpty());
        d.setEmail("");
        assertTrue(edicion(d).isEmpty()); // vacio = no cambiar
        d.setEmail("no-es-correo");
        d.setFechaNacimiento(LocalDate.now().plusDays(1));
        d.setNombre("n".repeat(256));
        assertEquals(Set.of("email", "fechaNacimiento", "nombre"), edicion(d));
    }

    // ---------- Vincular usuario ----------

    @Test
    void vincular_exige_usuario() {
        VincularDeportistaUsuarioDTO d = new VincularDeportistaUsuarioDTO();
        assertEquals(Set.of("usuarioId"), edicion(d));
        d.setUsuarioId(5);
        assertTrue(edicion(d).isEmpty());
    }

    // ---------- Historial médico ----------

    @Test
    void historial_medico_exige_descripcion_y_fecha_y_limita_textos() {
        HistorialMedicoDTO d = new HistorialMedicoDTO();
        assertEquals(Set.of("descripcion", "fechaRegistro"), edicion(d));
        d.setDescripcion("Esguince");
        d.setFechaRegistro(LocalDate.of(2026, 11, 1));
        assertTrue(edicion(d).isEmpty());
        d.setGravedad("g".repeat(256));
        d.setMedicoTratante("m".repeat(256));
        d.setTipoRegistro("t".repeat(256));
        d.setDescripcion("d".repeat(256));
        assertEquals(Set.of("gravedad", "medicoTratante", "tipoRegistro", "descripcion"), edicion(d));
    }
}
