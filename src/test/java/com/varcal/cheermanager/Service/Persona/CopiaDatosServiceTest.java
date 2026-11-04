package com.varcal.cheermanager.Service.Persona;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.Models.Historiales.HistorialMedico;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Consentimientos.ConsentimientoService;
import com.varcal.cheermanager.repository.Evaluaciones.EvaluacionRepository;
import com.varcal.cheermanager.repository.Evaluaciones.ObjetivoRepository;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
import com.varcal.cheermanager.repository.Financiero.PagoRepository;
import com.varcal.cheermanager.repository.Historiales.HistorialMedicoRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.AsistenciaRepository;
import com.varcal.cheermanager.repository.Org_dep.InscripcionRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaAcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** L6: la copia de datos de un titular reúne todas las secciones, incluye lo sensible y no inventa datos. */
class CopiaDatosServiceTest {

    static class ConsentFalso extends ConsentimientoService {
        ConsentFalso() { super(null, null, null, null, false); }

        @Override
        public List<Resumen> listarDePersona(Integer personaId) {
            return List.of(new Resumen(1, personaId, com.varcal.cheermanager.Models.Consentimientos.Consentimiento.Tipo.IMAGEN,
                    "2026-11", "DIGITAL", null, java.time.LocalDateTime.of(2026, 11, 4, 10, 0), null, null, true));
        }
    }

    static Persona persona() {
        Persona p = new Persona();
        p.setId(100);
        p.setNombre("Ana");
        p.setApellidos("Pérez");
        p.setNumeroDocumento("123");
        p.setTipoDocumento("TI");
        p.setFechaNacimiento(LocalDate.of(2015, 3, 2));
        return p;
    }

    CopiaDatosService servicio(boolean esDeportista) {
        Persona p = persona();
        Deportista d = new Deportista();
        d.setId(7);
        d.setPersona(p);
        d.setAltura(150);
        HistorialMedico h = new HistorialMedico();
        h.setDescripcion("Esguince de tobillo");
        h.setTipoRegistro("LESION");
        h.setFechaRegistro(LocalDate.of(2026, 5, 1));
        Factura f = new Factura();
        f.setNumeroFactura("F-001");
        f.setFechaEmision(LocalDate.of(2026, 10, 1));
        f.setConcepto("Mensualidad octubre");
        f.setTotal(new BigDecimal("150000"));
        f.setAnulada(true);
        f.setMotivoAnulacion("Cobro duplicado");

        Repo personas = new Repo().on("findById", a -> Optional.ofNullable(((Integer) a[0]) == 100 ? p : null));
        Repo deportistas = new Repo().on("findByPersonaId", a -> esDeportista ? Optional.of(d) : Optional.empty());
        Repo vinculos = new Repo().on("findByDeportistaId", a -> new ArrayList<>());
        Repo historial = new Repo().on("findByPersonaId", a -> new PageImpl<>(List.of(h)));
        Repo facturas = new Repo().on("findByPersonaIdOrderByFechaEmisionDesc", a -> List.of(f));
        Repo pagos = new Repo().on("findByPersonaId", a -> new ArrayList<>());
        Repo insc = new Repo().on("findByDeportistaId", a -> new ArrayList<>());
        Repo asist = new Repo().on("findByDeportistaIdOrderByFechaDesc", a -> new ArrayList<>());
        Repo evals = new Repo().on("findByDeportistaIdOrderByFechaDesc", a -> new ArrayList<>());
        Repo objs = new Repo().on("findByDeportistaIdOrderByFechaCreacionDesc", a -> new ArrayList<>());
        return new CopiaDatosService(Fakes.proxy(PersonaRepository.class, personas),
                Fakes.proxy(DeportistaRepository.class, deportistas),
                Fakes.proxy(DeportistaAcudienteRepository.class, vinculos), new ConsentFalso(),
                Fakes.proxy(HistorialMedicoRepository.class, historial), Fakes.proxy(FacturaRepository.class, facturas),
                Fakes.proxy(PagoRepository.class, pagos), Fakes.proxy(InscripcionRepository.class, insc),
                Fakes.proxy(AsistenciaRepository.class, asist), Fakes.proxy(EvaluacionRepository.class, evals),
                Fakes.proxy(ObjetivoRepository.class, objs));
    }

    static CopiaDatosService.Seccion sec(CopiaDatosService.Copia c, String inicio) {
        return c.secciones().stream().filter(s -> s.titulo().startsWith(inicio)).findFirst().orElse(null);
    }

    @Test
    void deportista_incluye_todas_las_secciones() {
        CopiaDatosService.Copia c = servicio(true).copia(100);
        assertEquals("Ana Pérez", c.titular());
        for (String t : List.of("Datos personales", "Ficha deportiva", "Acudientes", "Inscripciones", "Asistencia",
                "Evaluaciones", "Objetivos", "Historial médico", "Autorizaciones", "Facturas", "Pagos")) {
            assertNotNull(sec(c, t), "falta la sección " + t);
        }
    }

    @Test
    void persona_sin_ficha_deportiva_no_inventa_secciones_deportivas() {
        CopiaDatosService.Copia c = servicio(false).copia(100);
        assertNull(sec(c, "Ficha deportiva"));
        assertNull(sec(c, "Acudientes"));
        assertNotNull(sec(c, "Historial médico"));
        assertNotNull(sec(c, "Facturas"));
    }

    @Test
    void incluye_datos_de_salud_facturas_anuladas_y_consentimientos() {
        CopiaDatosService.Copia c = servicio(true).copia(100);
        assertEquals("Esguince de tobillo", sec(c, "Historial médico").filas().get(0).get(2));
        List<String> fila = sec(c, "Facturas").filas().get(0);
        assertEquals("F-001", fila.get(0));
        assertTrue(fila.get(4).contains("Cobro duplicado"));
        assertEquals("IMAGEN", sec(c, "Autorizaciones").filas().get(0).get(0));
        assertEquals("123", sec(c, "Datos personales").filas().get(3).get(1));
    }

    @Test
    void cada_fila_tiene_tantas_celdas_como_columnas() {
        CopiaDatosService.Copia c = servicio(true).copia(100);
        for (CopiaDatosService.Seccion s : c.secciones()) {
            for (List<String> f : s.filas()) {
                assertEquals(s.columnas().size(), f.size(), "sección " + s.titulo());
            }
        }
    }

    @Test
    void persona_inexistente_falla() {
        assertThrows(IllegalArgumentException.class, () -> servicio(true).copia(999));
    }
}
