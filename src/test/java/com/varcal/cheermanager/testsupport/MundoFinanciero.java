package com.varcal.cheermanager.testsupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.varcal.cheermanager.Models.Financiero.EstadoPago;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.Models.Financiero.MetodoPago;
import com.varcal.cheermanager.Models.Financiero.Pago;
import com.varcal.cheermanager.Models.Financiero.TipoPago;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Caja.CajaService;
import com.varcal.cheermanager.Service.Financiero.FacturaService;
import com.varcal.cheermanager.Service.Financiero.PagoService;
import com.varcal.cheermanager.repository.Eventos.EventoRepository;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
import com.varcal.cheermanager.repository.Financiero.MetodoPagoRepository;
import com.varcal.cheermanager.repository.Financiero.PagoRepository;
import com.varcal.cheermanager.repository.Financiero.TipoPagoRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/**
 * Mini "base de datos" en memoria con facturas y pagos, y los Services reales de dinero
 * (FacturaService y PagoService) armados encima. Asi las pruebas ejercitan las reglas reales
 * (saldo, sobrepago, anulaciones, caja cerrada) y no solo llamadas simuladas.
 */
public class MundoFinanciero {

    public final List<Factura> facturas = new ArrayList<>();
    public final List<Pago> pagos = new ArrayList<>();
    public final Map<Integer, Persona> personas = new HashMap<>();
    public final Set<LocalDate> diasCerrados = new HashSet<>();
    public final Set<Integer> eventosExistentes = new HashSet<>();

    public final FacturaService facturaService;
    public FacturaRepository facturaRepository;
    public PersonaRepository personaRepository;
    public final PagoService pagoService;
    private int seqFactura = 1;
    private int seqPago = 1;

    /** CajaService falso: rechaza los dias marcados como cerrados (mismo mensaje de error del real no importa). */
    static class CajaFalsa extends CajaService {
        final Set<LocalDate> cerrados;
        CajaFalsa(Set<LocalDate> cerrados) { super(null, null, null); this.cerrados = cerrados; }
        @Override
        public void exigirDiaAbierto(LocalDate fecha) {
            if (cerrados.contains(fecha)) throw new IllegalStateException("La caja del " + fecha + " esta cerrada");
        }
    }

    public MundoFinanciero() {
        Repo personaRepo = new Repo().on("findById", a -> Optional.ofNullable(personas.get((Integer) a[0])));
        Repo eventoRepo = new Repo().on("existsById", a -> eventosExistentes.contains((Integer) a[0]));

        Repo facturaRepo = new Repo()
                .on("findByIdForUpdate", a -> buscarFactura((Integer) a[0]))
                .on("findById", a -> buscarFactura((Integer) a[0]))
                .on("existsById", a -> buscarFactura((Integer) a[0]).isPresent())
                .on("countByNumeroFacturaStartingWith", a -> facturas.stream()
                        .filter(f -> f.getNumeroFactura().startsWith((String) a[0])).count())
                .on("findByNumeroFactura", a -> facturas.stream()
                        .filter(f -> f.getNumeroFactura().equals(a[0])).findFirst())
                .on("findEnMora", a -> facturas.stream()
                        .filter(f -> !Boolean.TRUE.equals(f.getAnulada()))
                        .filter(f -> f.getFechaVencimiento() != null && f.getFechaVencimiento().isBefore((LocalDate) a[0]))
                        .filter(f -> f.getTotal().compareTo(pagadoDe(f.getId())) > 0)
                        .toList())
                .on("save", a -> {
                    Factura f = (Factura) a[0];
                    if (f.getId() == null) {
                        f.setId(seqFactura++);
                        facturas.add(f);
                    }
                    return f;
                });

        Repo pagoRepo = new Repo()
                .on("findById", a -> pagos.stream().filter(p -> p.getId().equals(a[0])).findFirst())
                .on("findAll", a -> new ArrayList<>(pagos))
                .on("findByFacturaId", a -> pagos.stream()
                        .filter(p -> p.getFactura().getId().equals(a[0])).toList())
                .on("sumarMontoPagadoPorFactura", a -> pagadoDe((Integer) a[0]))
                .on("pagadoPorFacturas", a -> {
                    Map<Integer, BigDecimal> m = new HashMap<>();
                    for (Object id : (java.util.Collection<?>) a[0]) m.put((Integer) id, pagadoDe((Integer) id));
                    return m;
                })
                .on("save", a -> {
                    Pago p = (Pago) a[0];
                    if (p.getId() == null) {
                        p.setId(seqPago++);
                        pagos.add(p);
                    }
                    return p;
                });

        Repo tipoRepo = new Repo().on("findById", a -> {
            TipoPago t = new TipoPago();
            t.setId((Integer) a[0]);
            t.setTipo("Mensualidad");
            return Optional.of(t);
        });
        Repo metodoRepo = new Repo().on("findById", a -> {
            MetodoPago m = new MetodoPago();
            m.setId((Integer) a[0]);
            m.setMetodo("Efectivo");
            return Optional.of(m);
        });

        PagoRepository pagoRepoProxy = Fakes.proxy(PagoRepository.class, pagoRepo);
        FacturaRepository facturaRepoProxy = Fakes.proxy(FacturaRepository.class, facturaRepo);
        facturaService = new FacturaService(facturaRepoProxy, Fakes.proxy(PersonaRepository.class, personaRepo),
                pagoRepoProxy, Fakes.proxy(EventoRepository.class, eventoRepo));

        this.facturaRepository = facturaRepoProxy;
        this.personaRepository = Fakes.proxy(PersonaRepository.class, personaRepo);
        pagoService = new PagoService();
        Fakes.inyectar(pagoService, "pagoRepository", pagoRepoProxy);
        Fakes.inyectar(pagoService, "facturaRepository", facturaRepoProxy);
        Fakes.inyectar(pagoService, "tipoPagoRepository", Fakes.proxy(TipoPagoRepository.class, tipoRepo));
        Fakes.inyectar(pagoService, "metodoPagoRepository", Fakes.proxy(MetodoPagoRepository.class, metodoRepo));
        Fakes.inyectar(pagoService, "cajaService", new CajaFalsa(diasCerrados));
    }

    private Optional<Factura> buscarFactura(Integer id) {
        return facturas.stream().filter(f -> f.getId().equals(id)).findFirst();
    }

    private BigDecimal pagadoDe(Integer facturaId) {
        BigDecimal s = BigDecimal.ZERO;
        for (Pago p : pagos) {
            if (p.getFactura().getId().equals(facturaId) && p.getEstado() == EstadoPago.Pagado) s = s.add(p.getMonto());
        }
        return s;
    }

    // ---- atajos de armado ----
    public Persona persona(int id, String nombre) {
        Persona p = new Persona();
        p.setId(id);
        p.setNombre(nombre);
        p.setApellidos("Prueba");
        personas.put(id, p);
        return p;
    }

    /** Crea una factura vigente directamente en la "BD". */
    public Factura factura(int personaId, String total, LocalDate vencimiento) {
        Factura f = new Factura();
        f.setId(seqFactura++);
        f.setNumeroFactura("FAC-2026-" + String.format("%04d", f.getId()));
        f.setPersona(personas.containsKey(personaId) ? personas.get(personaId) : persona(personaId, "P" + personaId));
        f.setTotal(new BigDecimal(total));
        f.setFechaEmision(LocalDate.now().minusDays(20));
        f.setFechaVencimiento(vencimiento);
        f.setAnulada(false);
        facturas.add(f);
        return f;
    }

    public Pago pagoDirecto(Factura f, String monto, EstadoPago estado, LocalDate fecha) {
        Pago p = new Pago();
        p.setId(seqPago++);
        p.setFactura(f);
        p.setMonto(new BigDecimal(monto));
        p.setEstado(estado);
        p.setFecha(fecha);
        TipoPago t = new TipoPago(); t.setId(1); t.setTipo("Mensualidad"); p.setTipoPago(t);
        MetodoPago m = new MetodoPago(); m.setId(1); m.setMetodo("Efectivo"); p.setMetodoPago(m);
        pagos.add(p);
        return p;
    }
}
