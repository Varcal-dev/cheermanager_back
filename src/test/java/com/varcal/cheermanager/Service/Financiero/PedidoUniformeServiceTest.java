package com.varcal.cheermanager.Service.Financiero;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.DTO.Financiero.FacturaManualDTO;
import com.varcal.cheermanager.DTO.Financiero.FacturaResponseDTO;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.Models.Financiero.PedidoUniforme;
import com.varcal.cheermanager.Models.Financiero.PedidoUniformeCobro;
import com.varcal.cheermanager.Models.Org_dep.DeportistaPerteneceGrupo;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.EstadoPersona;
import com.varcal.cheermanager.Models.Personas.FichaDeportiva;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.repository.Eventos.EventoRepository;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
import com.varcal.cheermanager.repository.Financiero.PagoRepository;
import com.varcal.cheermanager.repository.Financiero.PedidoUniformeCobroRepository;
import com.varcal.cheermanager.repository.Financiero.PedidoUniformeRepository;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;
import com.varcal.cheermanager.repository.Personas.FichaDeportivaRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** C9: resumen de tallas por grupo y pedido de uniformes cobrado en cuotas. */
class PedidoUniformeServiceTest {

    static class FacturaFalsa extends FacturaService {
        final List<FacturaManualDTO> creadas = new ArrayList<>();
        FacturaFalsa() { super(null, null, null, null); }

        @Override
        public FacturaResponseDTO crearManual(FacturaManualDTO dto) {
            creadas.add(dto);
            FacturaResponseDTO r = new FacturaResponseDTO();
            r.setId(1000 + creadas.size());
            r.setNumeroFactura("FAC-" + creadas.size());
            return r;
        }
    }

    final List<DeportistaPerteneceGrupo> miembros = new ArrayList<>();
    final List<FichaDeportiva> fichas = new ArrayList<>();
    final List<PedidoUniforme> pedidos = new ArrayList<>();
    final List<PedidoUniformeCobro> cobros = new ArrayList<>();
    final List<Factura> facturasAnuladas = new ArrayList<>();
    FacturaFalsa facturaService;

    static DeportistaPerteneceGrupo miembro(int depId, int personaId, String nombre, String estado) {
        Persona p = new Persona();
        p.setId(personaId);
        p.setNombre(nombre);
        p.setApellidos("X");
        EstadoPersona ep = new EstadoPersona();
        ep.setEstado(estado);
        Deportista d = new Deportista();
        d.setId(depId);
        d.setPersona(p);
        d.setEstado(ep);
        DeportistaPerteneceGrupo m = new DeportistaPerteneceGrupo();
        m.setDeportista(d);
        return m;
    }

    static FichaDeportiva ficha(int depId, String uniforme, String calzado) {
        FichaDeportiva f = new FichaDeportiva();
        f.setDeportistaId(depId);
        f.setTallaUniforme(uniforme);
        f.setTallaCalzado(calzado);
        return f;
    }

    PedidoUniformeService servicio() {
        miembros.clear();
        fichas.clear();
        pedidos.clear();
        cobros.clear();
        facturasAnuladas.clear();
        facturaService = new FacturaFalsa();
        GrupoEntrenamiento g = new GrupoEntrenamiento();
        g.setId(3);
        g.setNombre("Senior N3");
        Repo grupos = new Repo().on("findById", a -> Optional.ofNullable(((Integer) a[0]) == 3 ? g : null));
        Repo mem = new Repo().on("findByGrupoIdAndFechaFinIsNull", a -> new ArrayList<>(miembros));
        Repo fs = new Repo().on("findByDeportistaIdIn", a -> new ArrayList<>(fichas));
        Repo peds = new Repo()
                .on("save", a -> { PedidoUniforme p = (PedidoUniforme) a[0]; if (p.getId() == null) { p.setId(pedidos.size() + 1); pedidos.add(p); } return p; })
                .on("findById", a -> pedidos.stream().filter(p -> p.getId().equals(a[0])).findFirst())
                .on("findAllByOrderByFechaCreacionDescIdDesc", a -> new ArrayList<>(pedidos));
        Repo cob = new Repo()
                .on("save", a -> { cobros.add((PedidoUniformeCobro) a[0]); return a[0]; })
                .on("findByPedidoId", a -> new ArrayList<>(cobros));
        Repo facs = new Repo().on("findAllById", a -> new ArrayList<>(facturasAnuladas));
        return new PedidoUniformeService(Fakes.proxy(PedidoUniformeRepository.class, peds),
                Fakes.proxy(PedidoUniformeCobroRepository.class, cob), Fakes.proxy(GrupoEntrenamientoRepository.class, grupos),
                Fakes.proxy(DeportistaPerteneceGrupoRepository.class, mem), Fakes.proxy(FichaDeportivaRepository.class, fs),
                Fakes.proxy(FacturaRepository.class, facs), facturaService);
    }

    PedidoUniformeService.Solicitud pedido(String valor, int cuotas) {
        return new PedidoUniformeService.Solicitud("Uniforme 2027", 3, new BigDecimal(valor), cuotas, LocalDate.now().plusDays(15));
    }

    void tresActivos() {
        miembros.add(miembro(1, 11, "Ana", "Activo"));
        miembros.add(miembro(2, 12, "Beto", "Activo"));
        miembros.add(miembro(3, 13, "Caro", "Retirado"));
        miembros.add(miembro(4, 14, "Dani", "Inactivo"));
        fichas.add(ficha(1, "YS", "34"));
        fichas.add(ficha(2, "YS", null));
    }

    @Test
    void resumen_de_tallas_cuenta_por_prenda_y_talla_y_excluye_retirados() {
        PedidoUniformeService s = servicio();
        tresActivos();
        miembros.add(miembro(5, 15, "Eli", "Activo")); // sin ficha
        PedidoUniformeService.ResumenTallas r = s.tallasDeGrupo(3);
        assertEquals("Senior N3", r.grupo());
        assertEquals(3, r.filas().size());
        assertEquals(2, r.conteo().get("uniforme").get("YS"));
        assertEquals(1, r.conteo().get("calzado").get("34"));
        assertEquals(1, r.sinTallaUniforme());
        assertEquals("Ana X", r.filas().get(0).nombre());
        assertFalse(r.filas().get(2).completa());
        assertThrows(IllegalArgumentException.class, () -> s.tallasDeGrupo(99));
        assertThrows(IllegalArgumentException.class, () -> s.tallasDeGrupo(null));
    }

    @Test
    void la_ultima_cuota_absorbe_el_redondeo() {
        List<BigDecimal> v = PedidoUniformeService.repartir(new BigDecimal("100000.00"), 3);
        assertEquals(new BigDecimal("33333.33"), v.get(0));
        assertEquals(new BigDecimal("33333.33"), v.get(1));
        assertEquals(new BigDecimal("33333.34"), v.get(2));
        assertEquals(0, new BigDecimal("100000.00").compareTo(v.stream().reduce(BigDecimal.ZERO, BigDecimal::add)));
        assertEquals(1, PedidoUniformeService.repartir(new BigDecimal("50000"), 1).size());
    }

    @Test
    void crear_valida_nombre_grupo_valor_cuotas_y_fecha() {
        PedidoUniformeService s = servicio();
        assertThrows(IllegalArgumentException.class, () -> s.crear(new PedidoUniformeService.Solicitud(" ", 3, BigDecimal.TEN, 2, null), 1));
        assertThrows(IllegalArgumentException.class, () -> s.crear(new PedidoUniformeService.Solicitud("U", 99, BigDecimal.TEN, 2, null), 1));
        assertThrows(IllegalArgumentException.class, () -> s.crear(new PedidoUniformeService.Solicitud("U", 3, BigDecimal.ZERO, 2, null), 1));
        assertThrows(IllegalArgumentException.class, () -> s.crear(pedido("90000", 0), 1));
        assertThrows(IllegalArgumentException.class, () -> s.crear(pedido("90000", 13), 1));
        assertThrows(IllegalArgumentException.class, () -> s.crear(new PedidoUniformeService.Solicitud("U", 3, new BigDecimal("0.01"), 3, null), 1));
        assertThrows(IllegalArgumentException.class,
                () -> s.crear(new PedidoUniformeService.Solicitud("U", 3, BigDecimal.TEN, 2, LocalDate.now().minusDays(1)), 1));
        assertTrue(pedidos.isEmpty());
        PedidoUniformeService.Resumen r = s.crear(pedido("90000", 3), 7);
        assertEquals("Senior N3", r.grupo());
        assertEquals(7, pedidos.get(0).getUsuarioId());
        assertEquals(1, s.listar().size());
    }

    @Test
    void facturar_crea_una_factura_por_cuota_y_deportista_activo_con_vencimientos_mensuales() {
        PedidoUniformeService s = servicio();
        tresActivos();
        s.crear(pedido("100000", 3), 1);
        PedidoUniformeService.Resultado r = s.facturar(1, false);
        assertEquals(2, r.facturados().size());       // Caro (retirada) y Dani (inactivo) fuera
        assertEquals(6, facturaService.creadas.size()); // 2 personas x 3 cuotas
        assertEquals(6, cobros.size());
        assertEquals(0, new BigDecimal("200000").compareTo(r.totalFacturado()));
        FacturaManualDTO c1 = facturaService.creadas.get(0), c3 = facturaService.creadas.get(2);
        assertEquals("UNIFORME", c1.getConcepto());
        assertEquals("Uniforme 2027 — cuota 1/3", c1.getDescripcion());
        assertEquals(new BigDecimal("33333.33"), c1.getTotal());
        assertEquals(new BigDecimal("33333.34"), c3.getTotal());
        assertEquals(c1.getFechaVencimiento().plusMonths(2), c3.getFechaVencimiento());
        assertEquals(0, r.sinTallaUniforme());
    }

    @Test
    void simular_no_crea_nada() {
        PedidoUniformeService s = servicio();
        tresActivos();
        s.crear(pedido("100000", 2), 1);
        PedidoUniformeService.Resultado r = s.facturar(1, true);
        assertTrue(r.simulacion());
        assertEquals(2, r.facturados().size());
        assertTrue(facturaService.creadas.isEmpty());
        assertTrue(cobros.isEmpty());
    }

    @Test
    void repetir_omite_a_quien_ya_tiene_sus_cuotas_y_cobra_al_que_entro_despues() {
        PedidoUniformeService s = servicio();
        tresActivos();
        s.crear(pedido("100000", 2), 1);
        s.facturar(1, false);
        miembros.add(miembro(6, 16, "Fer", "Activo"));
        PedidoUniformeService.Resultado r = s.facturar(1, false);
        assertEquals(1, r.facturados().size());
        assertEquals("Fer X", r.facturados().get(0).nombre());
        assertEquals(2, r.omitidos().size());
        assertEquals(6, facturaService.creadas.size()); // 4 de la 1.a pasada + 2 de Fer
        assertEquals(3, s.listar().get(0).deportistasFacturados());
    }

    @Test
    void si_las_facturas_fueron_anuladas_se_puede_volver_a_facturar() {
        PedidoUniformeService s = servicio();
        miembros.add(miembro(1, 11, "Ana", "Activo"));
        s.crear(pedido("60000", 2), 1);
        s.facturar(1, false);
        for (PedidoUniformeCobro c : cobros) {
            Factura f = new Factura();
            f.setId(c.getFacturaId());
            f.setAnulada(true);
            facturasAnuladas.add(f);
        }
        PedidoUniformeService.Resultado r = s.facturar(1, false);
        assertEquals(1, r.facturados().size());
        assertEquals(4, facturaService.creadas.size());
    }

    @Test
    void primer_vencimiento_pasado_vence_hoy_y_grupo_sin_activos_falla() {
        PedidoUniformeService s = servicio();
        miembros.add(miembro(1, 11, "Ana", "Activo"));
        s.crear(pedido("60000", 2), 1);
        pedidos.get(0).setPrimerVencimiento(LocalDate.now().minusDays(20));
        s.facturar(1, false);
        assertEquals(LocalDate.now(), facturaService.creadas.get(0).getFechaVencimiento());
        assertEquals(LocalDate.now().plusMonths(1), facturaService.creadas.get(1).getFechaVencimiento());

        PedidoUniformeService vacio = servicio();
        vacio.crear(pedido("60000", 2), 1);
        assertThrows(IllegalStateException.class, () -> vacio.facturar(1, false));
        assertThrows(IllegalArgumentException.class, () -> vacio.facturar(99, false));
    }
}
