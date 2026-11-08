package com.varcal.cheermanager.Service.Financiero;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Financiero.FacturaManualDTO;
import com.varcal.cheermanager.DTO.Financiero.FacturaResponseDTO;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.Models.Financiero.PedidoUniforme;
import com.varcal.cheermanager.Models.Financiero.PedidoUniformeCobro;
import com.varcal.cheermanager.Models.Org_dep.DeportistaPerteneceGrupo;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.FichaDeportiva;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
import com.varcal.cheermanager.repository.Financiero.PedidoUniformeCobroRepository;
import com.varcal.cheermanager.repository.Financiero.PedidoUniformeRepository;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;
import com.varcal.cheermanager.repository.Personas.FichaDeportivaRepository;

/**
 * Uniformes por grupo (C9): resumen de tallas para pedir al proveedor y cobro del pedido en cuotas.
 * Reglas del cobro (mismas que el cobro de evento por lote):
 *  - solo deportistas con membresia VIGENTE en el grupo; se omiten Retirados e Inactivos;
 *  - cada deportista recibe UNA factura por cuota (concepto UNIFORME); la ultima cuota absorbe el redondeo;
 *  - quien ya tiene sus cuotas vigentes (no anuladas) se omite: se puede repetir para cobrar a quien entro despues;
 *  - todo en una transaccion: se crean todas las facturas o ninguna.
 */
@Service
public class PedidoUniformeService {

    static final int MAX_CUOTAS = 12;
    static final List<String> PRENDAS = List.of("uniforme", "faldaShort", "camiseta", "buzo", "calzado");

    private final PedidoUniformeRepository pedidos;
    private final PedidoUniformeCobroRepository cobros;
    private final GrupoEntrenamientoRepository grupos;
    private final DeportistaPerteneceGrupoRepository membresias;
    private final FichaDeportivaRepository fichas;
    private final FacturaRepository facturas;
    private final FacturaService facturaService;

    public PedidoUniformeService(PedidoUniformeRepository pedidos, PedidoUniformeCobroRepository cobros,
            GrupoEntrenamientoRepository grupos, DeportistaPerteneceGrupoRepository membresias,
            FichaDeportivaRepository fichas, FacturaRepository facturas, FacturaService facturaService) {
        this.pedidos = pedidos;
        this.cobros = cobros;
        this.grupos = grupos;
        this.membresias = membresias;
        this.fichas = fichas;
        this.facturas = facturas;
        this.facturaService = facturaService;
    }

    public record FilaTallas(Integer deportistaId, Integer personaId, String nombre, String uniforme, String faldaShort,
            String camiseta, String buzo, String calzado, boolean completa) {
    }

    public record ResumenTallas(Integer grupoId, String grupo, List<FilaTallas> filas,
            Map<String, Map<String, Integer>> conteo, int sinTallaUniforme) {
    }

    public record Solicitud(String nombre, Integer grupoId, BigDecimal valorPorDeportista, Integer numeroCuotas,
            LocalDate primerVencimiento) {
    }

    public record Resumen(Integer id, String nombre, Integer grupoId, String grupo, BigDecimal valorPorDeportista,
            Integer numeroCuotas, LocalDate primerVencimiento, LocalDate fechaCreacion, long deportistasFacturados) {
    }

    public record Linea(Integer personaId, String nombre, List<String> facturas, String motivo) {
    }

    public record Resultado(boolean simulacion, Integer pedidoId, String nombre, BigDecimal valorPorDeportista,
            int cuotas, List<Linea> facturados, List<Linea> omitidos, BigDecimal totalFacturado,
            int sinTallaUniforme) {
    }

    /** Miembros vigentes del grupo que no estan Retirados ni Inactivos (una fila por persona). */
    private List<Deportista> activosDelGrupo(Integer grupoId) {
        Map<Integer, Deportista> porPersona = new LinkedHashMap<>();
        for (DeportistaPerteneceGrupo m : membresias.findByGrupoIdAndFechaFinIsNull(grupoId)) {
            Deportista d = m.getDeportista();
            if (d == null || d.getPersona() == null) continue;
            String estado = d.getEstado() == null || d.getEstado().getEstado() == null ? ""
                    : d.getEstado().getEstado().trim();
            if (estado.equalsIgnoreCase("Retirado") || estado.equalsIgnoreCase("Inactivo")) continue;
            porPersona.putIfAbsent(d.getPersona().getId(), d);
        }
        return new ArrayList<>(porPersona.values());
    }

    private static String nombre(Persona p) {
        return (p.getNombre() + " " + (p.getApellidos() == null ? "" : p.getApellidos())).trim();
    }

    private GrupoEntrenamiento grupo(Integer grupoId) {
        if (grupoId == null) throw new IllegalArgumentException("Indique el grupo.");
        return grupos.findById(grupoId).orElseThrow(() -> new IllegalArgumentException("Grupo no encontrado."));
    }

    /** Tallas de cada deportista activo del grupo y conteo por prenda y talla (para pedir al proveedor). */
    public ResumenTallas tallasDeGrupo(Integer grupoId) {
        GrupoEntrenamiento g = grupo(grupoId);
        List<Deportista> activos = activosDelGrupo(grupoId);
        Map<Integer, FichaDeportiva> fichaPorDeportista = new HashMap<>();
        if (!activos.isEmpty()) {
            for (FichaDeportiva f : fichas.findByDeportistaIdIn(activos.stream().map(Deportista::getId).toList())) {
                fichaPorDeportista.put(f.getDeportistaId(), f);
            }
        }
        List<FilaTallas> filas = new ArrayList<>();
        Map<String, Map<String, Integer>> conteo = new LinkedHashMap<>();
        PRENDAS.forEach(p -> conteo.put(p, new TreeMap<>()));
        int sinUniforme = 0;
        for (Deportista d : activos) {
            FichaDeportiva f = fichaPorDeportista.get(d.getId());
            String[] t = f == null ? new String[5]
                    : new String[] { f.getTallaUniforme(), f.getTallaFaldaShort(), f.getTallaCamiseta(),
                            f.getTallaBuzo(), f.getTallaCalzado() };
            for (int i = 0; i < PRENDAS.size(); i++) {
                if (t[i] != null) conteo.get(PRENDAS.get(i)).merge(t[i], 1, Integer::sum);
            }
            if (t[0] == null) sinUniforme++;
            // "Completa" = tiene la talla del uniforme de competencia (lo imprescindible para el pedido).
            filas.add(new FilaTallas(d.getId(), d.getPersona().getId(), nombre(d.getPersona()), t[0], t[1], t[2], t[3],
                    t[4], t[0] != null));
        }
        filas.sort((a, b) -> a.nombre().compareToIgnoreCase(b.nombre()));
        return new ResumenTallas(g.getId(), g.getNombre(), filas, conteo, sinUniforme);
    }

    @Transactional
    public Resumen crear(Solicitud s, Integer usuarioId) {
        if (s == null || s.nombre() == null || s.nombre().isBlank()) {
            throw new IllegalArgumentException("Escriba el nombre del pedido.");
        }
        if (s.nombre().trim().length() > 120) {
            throw new IllegalArgumentException("El nombre del pedido admite hasta 120 caracteres.");
        }
        GrupoEntrenamiento g = grupo(s.grupoId());
        if (s.valorPorDeportista() == null || s.valorPorDeportista().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor por deportista debe ser mayor a cero.");
        }
        int cuotas = s.numeroCuotas() == null ? 1 : s.numeroCuotas();
        if (cuotas < 1 || cuotas > MAX_CUOTAS) {
            throw new IllegalArgumentException("Las cuotas deben estar entre 1 y " + MAX_CUOTAS + ".");
        }
        BigDecimal valor = s.valorPorDeportista().setScale(2, RoundingMode.HALF_UP);
        if (valor.divide(BigDecimal.valueOf(cuotas), 2, RoundingMode.DOWN).compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor es muy bajo para dividirlo en " + cuotas + " cuotas.");
        }
        LocalDate hoy = LocalDate.now();
        LocalDate primero = s.primerVencimiento() == null ? FacturaService.calcularVencimiento(hoy) : s.primerVencimiento();
        if (primero.isBefore(hoy)) {
            throw new IllegalArgumentException("El primer vencimiento no puede ser anterior a hoy.");
        }
        PedidoUniforme p = new PedidoUniforme();
        p.setNombre(s.nombre().trim());
        p.setGrupoId(g.getId());
        p.setValorPorDeportista(valor);
        p.setNumeroCuotas(cuotas);
        p.setPrimerVencimiento(primero);
        p.setFechaCreacion(hoy);
        p.setUsuarioId(usuarioId);
        p = pedidos.save(p);
        return resumen(p, g.getNombre(), 0);
    }

    public List<Resumen> listar() {
        List<Resumen> out = new ArrayList<>();
        for (PedidoUniforme p : pedidos.findAllByOrderByFechaCreacionDescIdDesc()) {
            String nombreGrupo = grupos.findById(p.getGrupoId()).map(GrupoEntrenamiento::getNombre).orElse("—");
            long facturados = cobros.findByPedidoId(p.getId()).stream().map(PedidoUniformeCobro::getPersonaId)
                    .distinct().count();
            out.add(resumen(p, nombreGrupo, facturados));
        }
        return out;
    }

    private static Resumen resumen(PedidoUniforme p, String grupo, long facturados) {
        return new Resumen(p.getId(), p.getNombre(), p.getGrupoId(), grupo, p.getValorPorDeportista(),
                p.getNumeroCuotas(), p.getPrimerVencimiento(), p.getFechaCreacion(), facturados);
    }

    /** Valor de cada cuota: se reparte en partes iguales y la ultima absorbe el redondeo. */
    static List<BigDecimal> repartir(BigDecimal total, int cuotas) {
        BigDecimal base = total.divide(BigDecimal.valueOf(cuotas), 2, RoundingMode.DOWN);
        List<BigDecimal> out = new ArrayList<>();
        BigDecimal acumulado = BigDecimal.ZERO;
        for (int i = 1; i < cuotas; i++) {
            out.add(base);
            acumulado = acumulado.add(base);
        }
        out.add(total.subtract(acumulado));
        return out;
    }

    /** Factura las cuotas del pedido a los deportistas activos del grupo (simular=true: solo vista previa). */
    @Transactional
    public Resultado facturar(Integer pedidoId, boolean simular) {
        PedidoUniforme p = pedidos.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado."));
        GrupoEntrenamiento g = grupo(p.getGrupoId());
        List<Deportista> activos = activosDelGrupo(p.getGrupoId());
        if (activos.isEmpty()) {
            throw new IllegalStateException("El grupo no tiene deportistas activos para facturar.");
        }
        Map<Integer, FichaDeportiva> fichaPorDeportista = new HashMap<>();
        for (FichaDeportiva f : fichas.findByDeportistaIdIn(activos.stream().map(Deportista::getId).toList())) {
            fichaPorDeportista.put(f.getDeportistaId(), f);
        }

        // Personas que ya tienen cuotas VIGENTES de este pedido (una factura anulada no cuenta).
        List<PedidoUniformeCobro> previos = cobros.findByPedidoId(p.getId());
        Set<Integer> anuladas = new HashSet<>();
        if (!previos.isEmpty()) {
            for (Factura f : facturas.findAllById(previos.stream().map(PedidoUniformeCobro::getFacturaId).toList())) {
                if (Boolean.TRUE.equals(f.getAnulada())) anuladas.add(f.getId());
            }
        }
        Set<Integer> yaFacturadas = previos.stream().filter(c -> !anuladas.contains(c.getFacturaId()))
                .map(PedidoUniformeCobro::getPersonaId).collect(Collectors.toSet());

        LocalDate hoy = LocalDate.now();
        // Si el primer vencimiento ya paso (p. ej. alguien que entra despues), la 1.a cuota vence hoy.
        LocalDate base = p.getPrimerVencimiento().isBefore(hoy) ? hoy : p.getPrimerVencimiento();
        List<BigDecimal> valores = repartir(p.getValorPorDeportista(), p.getNumeroCuotas());

        List<Linea> facturados = new ArrayList<>();
        List<Linea> omitidos = new ArrayList<>();
        int sinTalla = 0;
        for (Deportista d : activos) {
            Persona persona = d.getPersona();
            FichaDeportiva f = fichaPorDeportista.get(d.getId());
            if (yaFacturadas.contains(persona.getId())) {
                omitidos.add(new Linea(persona.getId(), nombre(persona), List.of(), "Ya tiene las cuotas de este pedido"));
                continue;
            }
            if (f == null || f.getTallaUniforme() == null) sinTalla++;
            List<String> numeros = new ArrayList<>();
            for (int i = 1; i <= p.getNumeroCuotas(); i++) {
                if (simular) {
                    numeros.add("cuota " + i + "/" + p.getNumeroCuotas());
                    continue;
                }
                FacturaManualDTO dto = new FacturaManualDTO();
                dto.setPersonaId(persona.getId());
                dto.setConcepto("UNIFORME");
                dto.setDescripcion(p.getNombre() + " — cuota " + i + "/" + p.getNumeroCuotas());
                dto.setTotal(valores.get(i - 1));
                dto.setFechaEmision(hoy);
                dto.setFechaVencimiento(base.plusMonths(i - 1L));
                FacturaResponseDTO creada = facturaService.crearManual(dto);
                PedidoUniformeCobro c = new PedidoUniformeCobro();
                c.setPedidoId(p.getId());
                c.setPersonaId(persona.getId());
                c.setCuota(i);
                c.setFacturaId(creada.getId());
                cobros.save(c);
                numeros.add(creada.getNumeroFactura());
            }
            facturados.add(new Linea(persona.getId(), nombre(persona), numeros, null));
        }
        BigDecimal total = p.getValorPorDeportista().multiply(BigDecimal.valueOf(facturados.size()));
        return new Resultado(simular, p.getId(), p.getNombre(), p.getValorPorDeportista(), p.getNumeroCuotas(),
                facturados, omitidos, total, sinTalla);
    }
}
