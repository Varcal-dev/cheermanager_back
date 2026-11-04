package com.varcal.cheermanager.Service.Financiero;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.DTO.Financiero.CobroEventoLoteDTO;
import com.varcal.cheermanager.DTO.Financiero.CobroEventoLoteResultadoDTO;
import com.varcal.cheermanager.DTO.Financiero.FacturaManualDTO;
import com.varcal.cheermanager.DTO.Financiero.FacturaResponseDTO;
import com.varcal.cheermanager.Models.Eventos.Evento;
import com.varcal.cheermanager.Models.Eventos.GrupoEvento;
import com.varcal.cheermanager.Models.Org_dep.DeportistaPerteneceGrupo;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.EstadoPersona;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.repository.Eventos.EventoRepository;
import com.varcal.cheermanager.repository.Eventos.GrupoEventoRepository;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** Cobro de un evento por lote: sin duplicados, omisiones y vista previa. Sin base de datos. */
class CobroEventoServiceTest {

    /** FacturaService falso: solo registra lo que se le pide crear. */
    static class FacturaServiceFalso extends FacturaService {
        final List<FacturaManualDTO> creadas = new ArrayList<>();
        FacturaServiceFalso() { super(null, null, null, null); }
        @Override
        public FacturaResponseDTO crearManual(FacturaManualDTO dto) {
            creadas.add(dto);
            FacturaResponseDTO r = new FacturaResponseDTO();
            r.setNumeroFactura("FAC-2026-" + creadas.size());
            return r;
        }
    }

    // ---- armado de datos ----
    static Persona persona(int id, String nombre) {
        Persona p = new Persona();
        p.setId(id);
        p.setNombre(nombre);
        p.setApellidos("Prueba");
        return p;
    }

    static DeportistaPerteneceGrupo miembro(int personaId, String nombre, String estado) {
        Deportista d = new Deportista();
        d.setId(personaId);
        d.setPersona(persona(personaId, nombre));
        EstadoPersona e = new EstadoPersona();
        e.setEstado(estado);
        d.setEstado(e);
        DeportistaPerteneceGrupo m = new DeportistaPerteneceGrupo();
        m.setDeportista(d);
        return m;
    }

    static GrupoEvento grupoEvento(int grupoId, String nombre) {
        GrupoEntrenamiento g = new GrupoEntrenamiento();
        g.setId(grupoId);
        g.setNombre(nombre);
        GrupoEvento ge = new GrupoEvento();
        ge.setGrupoEntrenamiento(g);
        return ge;
    }

    /** Escenario: evento 7 con dos grupos; Ana esta en ambos, Beto Retirado, Cata ya cobrada. */
    static class Escenario {
        final FacturaServiceFalso facturas = new FacturaServiceFalso();
        final Repo facturaRepo;
        final CobroEventoService servicio;

        Escenario(boolean eventoExiste, List<GrupoEvento> grupos, List<Integer> yaCobrados) {
            Evento ev = new Evento();
            ev.setId(7);
            ev.setNombre("Nacional");
            Repo eventoRepo = new Repo().on("findByIdForUpdate",
                    a -> eventoExiste ? Optional.of(ev) : Optional.empty());
            Repo geRepo = new Repo().on("findByEventoId", a -> grupos);
            Repo membRepo = new Repo().on("findByGrupoIdAndFechaFinIsNull", a -> {
                int gid = (Integer) a[0];
                List<DeportistaPerteneceGrupo> l = new ArrayList<>();
                if (gid == 1) {
                    l.add(miembro(10, "Ana", "Activo"));
                    l.add(miembro(11, "Beto", "Retirado"));
                    l.add(miembro(12, "Cata", "Activo"));
                } else if (gid == 2) {
                    l.add(miembro(10, "Ana", "Activo")); // repetida: otro grupo del mismo evento
                    l.add(miembro(13, "Dani", "Inactivo"));
                    l.add(miembro(14, "Eli", "Activo"));
                }
                return l;
            });
            facturaRepo = new Repo().on("findPersonaIdsConCobroDeEvento", a -> yaCobrados);
            servicio = new CobroEventoService(
                    Fakes.proxy(EventoRepository.class, eventoRepo),
                    Fakes.proxy(GrupoEventoRepository.class, geRepo),
                    Fakes.proxy(DeportistaPerteneceGrupoRepository.class, membRepo),
                    Fakes.proxy(FacturaRepository.class, facturaRepo),
                    facturas);
        }
    }

    static List<GrupoEvento> dosGrupos() {
        return List.of(grupoEvento(1, "Senior"), grupoEvento(2, "Junior"));
    }

    static CobroEventoLoteDTO dto(String monto) {
        CobroEventoLoteDTO d = new CobroEventoLoteDTO();
        if (monto != null) d.setMonto(new BigDecimal(monto));
        return d;
    }

    @Test
    void cobraUnaVezACadaPersonaAunqueEsteEnVariosGrupos() {
        Escenario e = new Escenario(true, dosGrupos(), List.of());
        CobroEventoLoteResultadoDTO r = e.servicio.cobrar(7, dto("50000"));
        // Ana (en 2 grupos), Cata y Eli. Beto (Retirado) y Dani (Inactivo) se omiten.
        assertEquals(3, r.totalACobrar());
        assertEquals(3, e.facturas.creadas.size());
        long anas = e.facturas.creadas.stream().filter(f -> f.getPersonaId() == 10).count();
        assertEquals(1, (int) anas);
        assertTrue(Fakes.igual("150000", r.valorTotal()));
    }

    @Test
    void omiteRetiradosEInactivosConSuMotivo() {
        Escenario e = new Escenario(true, dosGrupos(), List.of());
        CobroEventoLoteResultadoDTO r = e.servicio.cobrar(7, dto("50000"));
        assertEquals(2, r.omitidos().size());
        assertTrue(r.omitidos().stream().anyMatch(l -> l.personaId() == 11 && l.motivo().toLowerCase().contains("retirado")));
        assertTrue(r.omitidos().stream().anyMatch(l -> l.personaId() == 13 && l.motivo().toLowerCase().contains("inactivo")));
    }

    @Test
    void repetirLaOperacionNoDuplicaLosCobrosVigentes() {
        // Ana y Cata ya tienen cobro vigente: solo Eli queda por cobrar.
        Escenario e = new Escenario(true, dosGrupos(), List.of(10, 12));
        CobroEventoLoteResultadoDTO r = e.servicio.cobrar(7, dto("50000"));
        assertEquals(1, r.totalACobrar());
        assertEquals(14, (int) e.facturas.creadas.get(0).getPersonaId());
        assertEquals(2, r.yaCobrados());
    }

    @Test
    void simularNoCreaNingunaFactura() {
        Escenario e = new Escenario(true, dosGrupos(), List.of());
        CobroEventoLoteDTO d = dto("50000");
        d.setSimular(true);
        CobroEventoLoteResultadoDTO r = e.servicio.cobrar(7, d);
        assertTrue(r.simulacion());
        assertEquals(3, r.totalACobrar());
        assertEquals(0, e.facturas.creadas.size());
        assertNull(r.cobrados().get(0).numeroFactura());
    }

    @Test
    void lasFacturasSalenConceptoEventoMontoYEventoLigado() {
        Escenario e = new Escenario(true, dosGrupos(), List.of());
        e.servicio.cobrar(7, dto("50000.499"));
        FacturaManualDTO f = e.facturas.creadas.get(0);
        assertEquals("EVENTO", f.getConcepto());
        assertEquals(7, (int) f.getEventoId());
        assertTrue(Fakes.igual("50000.50", f.getTotal()), "el monto se redondea a 2 decimales");
        assertEquals("Evento: Nacional", f.getDescripcion());
        assertFalse(f.getFechaVencimiento().isBefore(LocalDate.now()));
    }

    @Test
    void soloCobraLosGruposPedidosYTodosDebenEstarInscritos() {
        Escenario e = new Escenario(true, dosGrupos(), List.of());
        CobroEventoLoteDTO d = dto("50000");
        d.setGrupoIds(List.of(2));
        CobroEventoLoteResultadoDTO r = e.servicio.cobrar(7, d);
        // Grupo 2 = Ana, Dani (Inactivo, omitido) y Eli: se cobra a Ana y Eli, y no a Cata (solo grupo 1).
        assertEquals(2, r.totalACobrar());
        assertTrue(e.facturas.creadas.stream().noneMatch(f -> f.getPersonaId() == 12));

        Escenario e2 = new Escenario(true, dosGrupos(), List.of());
        CobroEventoLoteDTO malo = dto("50000");
        malo.setGrupoIds(List.of(99));
        assertThrows(RuntimeException.class, () -> e2.servicio.cobrar(7, malo));
        assertEquals(0, e2.facturas.creadas.size());
    }

    @Test
    void montoInvalidoEventoInexistenteOSinGruposFallanSinCrearNada() {
        Escenario e = new Escenario(true, dosGrupos(), List.of());
        assertThrows(RuntimeException.class, () -> e.servicio.cobrar(7, dto(null)));
        assertThrows(RuntimeException.class, () -> e.servicio.cobrar(7, dto("0")));
        assertThrows(RuntimeException.class, () -> e.servicio.cobrar(7, dto("-5")));
        assertThrows(RuntimeException.class, () -> e.servicio.cobrar(7, null));

        Escenario sinEvento = new Escenario(false, dosGrupos(), List.of());
        assertThrows(RuntimeException.class, () -> sinEvento.servicio.cobrar(7, dto("100")));

        Escenario sinGrupos = new Escenario(true, List.of(), List.of());
        assertThrows(RuntimeException.class, () -> sinGrupos.servicio.cobrar(7, dto("100")));

        assertEquals(0, e.facturas.creadas.size());
        assertEquals(0, sinEvento.facturas.creadas.size());
        assertEquals(0, sinGrupos.facturas.creadas.size());
    }

    @Test
    void vencimientoAnteriorAHoyEsRechazado() {
        Escenario e = new Escenario(true, dosGrupos(), List.of());
        CobroEventoLoteDTO d = dto("100");
        d.setFechaVencimiento(LocalDate.now().minusDays(1));
        assertThrows(RuntimeException.class, () -> e.servicio.cobrar(7, d));
        assertEquals(0, e.facturas.creadas.size());
    }
}
