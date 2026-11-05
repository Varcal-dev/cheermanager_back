package com.varcal.cheermanager.Service.Persona;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.Models.Historiales.HistorialMedico;
import com.varcal.cheermanager.Models.Legal.Anonimizacion;
import com.varcal.cheermanager.Models.Personas.Acudiente;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.DeportistaAcudiente;
import com.varcal.cheermanager.Models.Personas.EstadoPersona;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
import com.varcal.cheermanager.repository.Financiero.PagoRepository;
import com.varcal.cheermanager.repository.Historiales.HistorialMedicoRepository;
import com.varcal.cheermanager.repository.Legal.AnonimizacionRepository;
import com.varcal.cheermanager.repository.Personas.AcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaAcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** L7: anonimizar borra lo identificable, conserva la contabilidad y se niega cuando no debe. */
class AnonimizacionServiceTest {

    // Mundo de la prueba (se reconstruye en cada llamada a armar()).
    Persona persona;
    Deportista deportista;
    Usuario usuarioDeportista;
    Acudiente acudienteExclusivo;
    Acudiente acudienteCompartido;
    List<Factura> facturas;
    Map<Integer, BigDecimal> pagado;
    List<HistorialMedico> medico;
    List<Object> medicoBorrado;
    List<Anonimizacion> constancias;
    List<Usuario> usuariosGuardados;
    Repo repoHistorial;
    AnonimizacionService servicio;

    static Factura factura(int id, String total, boolean anulada) {
        Factura f = new Factura();
        f.setId(id);
        f.setTotal(new BigDecimal(total));
        f.setAnulada(anulada);
        return f;
    }

    void armar(String estado) {
        persona = new Persona();
        persona.setId(100);
        persona.setNombre("Ana");
        persona.setApellidos("Pérez");
        persona.setNumeroDocumento("123");
        persona.setDireccion("Calle 1");
        persona.setTelefono("300");
        persona.setFechaNacimiento(LocalDate.of(2015, 3, 2));

        EstadoPersona ep = new EstadoPersona();
        ep.setEstado(estado);
        deportista = new Deportista();
        deportista.setId(7);
        deportista.setPersona(persona);
        deportista.setEstado(ep);
        deportista.setContactoEmergencia("Mamá 300");
        deportista.setFotoPerfil("foto.png");
        deportista.setPeso(40);
        deportista.setAltura(150);

        usuarioDeportista = new Usuario();
        usuarioDeportista.setId(50);
        usuarioDeportista.setEmail("ana@x.com");
        usuarioDeportista.setUsername("ana");
        usuarioDeportista.setPasswordHash("hash");
        usuarioDeportista.setPersona(persona);

        acudienteExclusivo = new Acudiente();
        acudienteExclusivo.setId(1);
        acudienteExclusivo.setNombre("Luis");
        acudienteExclusivo.setApellidos("Pérez");
        acudienteExclusivo.setEmail("luis@x.com");
        acudienteExclusivo.setTelefono("301");
        acudienteExclusivo.setUsuarioId(60);
        acudienteCompartido = new Acudiente();
        acudienteCompartido.setId(2);
        acudienteCompartido.setNombre("Marta");
        acudienteCompartido.setApellidos("Gómez");
        acudienteCompartido.setEmail("marta@x.com");

        Usuario usuarioAcudiente = new Usuario();
        usuarioAcudiente.setId(60);
        usuarioAcudiente.setEmail("luis@x.com");

        DeportistaAcudiente v1 = new DeportistaAcudiente();
        v1.setAcudiente(acudienteExclusivo);
        v1.setDeportista(deportista);
        DeportistaAcudiente v2 = new DeportistaAcudiente();
        v2.setAcudiente(acudienteCompartido);
        v2.setDeportista(deportista);
        Deportista hermano = new Deportista();
        hermano.setId(8);

        facturas = new ArrayList<>();
        pagado = new HashMap<>();
        medico = new ArrayList<>(List.of(new HistorialMedico(), new HistorialMedico()));
        medicoBorrado = new ArrayList<>();
        constancias = new ArrayList<>();
        usuariosGuardados = new ArrayList<>();

        Repo personas = new Repo().on("findById", a -> Optional.of(persona)).on("save", a -> a[0]);
        Repo deportistas = new Repo().on("findByPersonaId", a -> Optional.of(deportista)).on("save", a -> a[0]);
        Repo usuarios = new Repo()
                .on("findById", a -> {
                    long id = (Long) a[0];
                    return Optional.ofNullable(id == 60 ? usuarioAcudiente : id == 99 ? ejecutor() : null);
                })
                .on("findByPersona", a -> Optional.of(usuarioDeportista))
                .on("save", a -> { usuariosGuardados.add((Usuario) a[0]); return a[0]; });
        Repo acudientes = new Repo().on("save", a -> a[0]);
        Repo vinculos = new Repo()
                .on("findByDeportistaId", a -> List.of(v1, v2))
                .on("deportistasDeAcudiente", a -> ((Integer) a[0]) == 1 ? List.of(deportista) : List.of(deportista, hermano));
        repoHistorial = new Repo()
                .on("findByPersonaId", a -> new PageImpl<>(medico))
                .on("deleteAll", a -> { medicoBorrado.addAll((List<?>) a[0]); return null; });
        Repo facs = new Repo().on("findByPersonaIdOrderByFechaEmisionDesc", a -> facturas);
        Repo pagos = new Repo().on("pagadoPorFacturas", a -> pagado);
        Repo anon = new Repo().on("save", a -> { constancias.add((Anonimizacion) a[0]); return a[0]; });

        servicio = new AnonimizacionService(Fakes.proxy(PersonaRepository.class, personas),
                Fakes.proxy(DeportistaRepository.class, deportistas), Fakes.proxy(UserRepository.class, usuarios),
                Fakes.proxy(AcudienteRepository.class, acudientes),
                Fakes.proxy(DeportistaAcudienteRepository.class, vinculos),
                Fakes.proxy(HistorialMedicoRepository.class, repoHistorial), Fakes.proxy(FacturaRepository.class, facs),
                Fakes.proxy(PagoRepository.class, pagos), Fakes.proxy(AnonimizacionRepository.class, anon));
    }

    Usuario ejecutor() {
        Usuario u = new Usuario();
        u.setId(99);
        Persona otra = new Persona();
        otra.setId(555);
        u.setPersona(otra);
        return u;
    }

    static AnonimizacionService.Solicitud ok() {
        return new AnonimizacionService.Solicitud("Solicitud del titular #12", "ANONIMIZAR");
    }

    @Test
    void anonimiza_persona_ficha_acceso_y_acudiente_exclusivo() {
        armar("Retirado");
        AnonimizacionService.Resultado r = servicio.anonimizar(100, ok(), 99);
        assertEquals("Titular anonimizado", persona.getNombre());
        assertEquals("#100", persona.getApellidos());
        assertEquals("ANON-100", persona.getNumeroDocumento());
        assertNull(persona.getDireccion());
        assertNull(persona.getTelefono());
        assertEquals(LocalDate.of(2015, 1, 1), persona.getFechaNacimiento());
        assertNull(deportista.getContactoEmergencia());
        assertNull(deportista.getFotoPerfil());
        assertNull(deportista.getPeso());
        assertEquals(2, r.registrosMedicosEliminados());
        assertEquals(2, medicoBorrado.size());
        assertTrue(r.accesoDesactivado());
        assertFalse(usuarioDeportista.getActivo());
        assertTrue(usuarioDeportista.getEmail().endsWith("@anonimizado.invalid"));
        assertEquals("!", usuarioDeportista.getPasswordHash());
        // Acudiente exclusivo anonimizado (y su acceso); el compartido queda intacto.
        assertEquals(1, r.acudientesAnonimizados());
        assertEquals("Acudiente anonimizado", acudienteExclusivo.getNombre());
        assertNull(acudienteExclusivo.getEmail());
        assertEquals("Marta", acudienteCompartido.getNombre());
        assertEquals("marta@x.com", acudienteCompartido.getEmail());
        assertEquals(2, usuariosGuardados.size());
    }

    @Test
    void deja_constancia_sin_datos_personales() {
        armar("Retirado");
        servicio.anonimizar(100, ok(), 99);
        assertEquals(1, constancias.size());
        assertEquals(100, constancias.get(0).getPersonaId());
        assertEquals(99, constancias.get(0).getUsuarioId());
        assertEquals("Solicitud del titular #12", constancias.get(0).getMotivo());
    }

    @Test
    void con_saldo_pendiente_se_rechaza_y_no_cambia_nada() {
        armar("Retirado");
        facturas.add(factura(1, "100000", false));
        pagado.put(1, new BigDecimal("40000"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> servicio.anonimizar(100, ok(), 99));
        assertTrue(ex.getMessage().contains("saldo pendiente"));
        assertEquals("Ana", persona.getNombre());
        assertTrue(medicoBorrado.isEmpty());
        assertTrue(constancias.isEmpty());
    }

    @Test
    void facturas_pagadas_o_anuladas_no_impiden() {
        armar("Retirado");
        facturas.add(factura(1, "100000", false));
        pagado.put(1, new BigDecimal("100000"));
        facturas.add(factura(2, "50000", true)); // anulada con saldo: no cuenta
        servicio.anonimizar(100, ok(), 99);
        assertEquals("Titular anonimizado", persona.getNombre());
    }

    @Test
    void solo_deportistas_retirados() {
        armar("Activo");
        assertThrows(IllegalStateException.class, () -> servicio.anonimizar(100, ok(), 99));
        assertEquals("Ana", persona.getNombre());
    }

    @Test
    void exige_motivo_y_palabra_de_confirmacion() {
        armar("Retirado");
        assertThrows(IllegalArgumentException.class,
                () -> servicio.anonimizar(100, new AnonimizacionService.Solicitud("ok", "ANONIMIZAR"), 99));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.anonimizar(100, new AnonimizacionService.Solicitud("Solicitud del titular", "si"), 99));
        assertThrows(IllegalArgumentException.class, () -> servicio.anonimizar(100, null, 99));
        assertEquals("Ana", persona.getNombre());
    }

    @Test
    void no_se_anonimiza_dos_veces() {
        armar("Retirado");
        servicio.anonimizar(100, ok(), 99);
        assertThrows(IllegalStateException.class, () -> servicio.anonimizar(100, ok(), 99));
        assertEquals(1, constancias.size());
    }

    @Test
    void no_puede_anonimizar_su_propia_persona() {
        armar("Retirado");
        persona.setId(555); // la persona del ejecutor (usuario 99)
        assertThrows(IllegalStateException.class, () -> servicio.anonimizar(555, ok(), 99));
        assertEquals("Ana", persona.getNombre());
    }
}
