package com.varcal.cheermanager.Service.Eventos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Eventos.Evento;
import com.varcal.cheermanager.Models.Eventos.RosterCompetencia;
import com.varcal.cheermanager.Models.Eventos.RosterIntegrante;
import com.varcal.cheermanager.Models.Eventos.ViajeAbono;
import com.varcal.cheermanager.Models.Eventos.ViajeCompetencia;
import com.varcal.cheermanager.Models.Eventos.ViajeCompetencia.Estado;
import com.varcal.cheermanager.Models.Eventos.ViajeParticipante;
import com.varcal.cheermanager.Models.Eventos.ViajeParticipante.RolAcompanante;
import com.varcal.cheermanager.Models.Eventos.ViajeParticipante.Tipo;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.repository.Eventos.EventoRepository;
import com.varcal.cheermanager.repository.Eventos.RosterCompetenciaRepository;
import com.varcal.cheermanager.repository.Eventos.RosterIntegranteRepository;
import com.varcal.cheermanager.repository.Eventos.ViajeAbonoRepository;
import com.varcal.cheermanager.repository.Eventos.ViajeCompetenciaRepository;
import com.varcal.cheermanager.repository.Eventos.ViajeParticipanteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

/**
 * C5: viaje a competencia. Delegacion (desde los rosters de C2) y acompanantes, autorizacion de viaje firmada para
 * los menores, abonos con saldo y cierre con las condiciones minimas. Los abonos son un registro interno: no
 * generan factura.
 */
@Service
public class ViajeService {

    public static final int MAYORIA_EDAD = 18;

    public record Solicitud(String destino, LocalDate fechaSalida, LocalDate fechaRegreso, String puntoEncuentro,
            String transporte, String alojamiento, BigDecimal costoPorPersona, String notas) {
    }

    public record Opcion(Integer eventoId, String evento, LocalDate fecha, Integer viajeId, String estado) {
    }

    public record Participante(Integer id, Tipo tipo, Integer deportistaId, String nombre, String documento,
            RolAcompanante rol, Integer edad, boolean menor, ViajeParticipante.Estado estado, boolean autorizacion,
            LocalDate fechaAutorizacion, String notas, BigDecimal costo, BigDecimal abonado, BigDecimal saldo) {
    }

    public record Resumen(int deportistas, int acompanantes, int confirmados, int pendientes, int noVan,
            int menoresSinAutorizacion, BigDecimal costoTotal, BigDecimal abonadoTotal, BigDecimal saldoTotal) {
    }

    public record Vista(Integer id, Integer eventoId, String evento, LocalDate fechaEvento, String destino,
            LocalDate fechaSalida, LocalDate fechaRegreso, String puntoEncuentro, String transporte, String alojamiento,
            BigDecimal costoPorPersona, String notas, Estado estado, Resumen resumen, List<Participante> participantes) {
    }

    private final ViajeCompetenciaRepository viajes;
    private final ViajeParticipanteRepository participantes;
    private final ViajeAbonoRepository abonos;
    private final EventoRepository eventos;
    private final DeportistaRepository deportistas;
    private final RosterCompetenciaRepository rosters;
    private final RosterIntegranteRepository integrantes;

    public ViajeService(ViajeCompetenciaRepository viajes, ViajeParticipanteRepository participantes, ViajeAbonoRepository abonos,
            EventoRepository eventos, DeportistaRepository deportistas, RosterCompetenciaRepository rosters,
            RosterIntegranteRepository integrantes) {
        this.viajes = viajes;
        this.participantes = participantes;
        this.abonos = abonos;
        this.eventos = eventos;
        this.deportistas = deportistas;
        this.rosters = rosters;
        this.integrantes = integrantes;
    }

    protected LocalDate hoy() { return LocalDate.now(); }
    protected LocalDateTime ahora() { return LocalDateTime.now(); }

    public List<Opcion> opciones() {
        Map<Integer, ViajeCompetencia> porEvento = new HashMap<>();
        for (ViajeCompetencia v : viajes.findAll()) porEvento.put(v.getEventoId(), v);
        List<Opcion> out = new ArrayList<>();
        for (Evento e : eventos.findAll()) {
            ViajeCompetencia v = porEvento.get(e.getId());
            out.add(new Opcion(e.getId(), e.getNombre(), e.getFecha(), v == null ? null : v.getId(), v == null ? "SIN_VIAJE" : v.getEstado().name()));
        }
        out.sort(java.util.Comparator.comparing((Opcion o) -> o.fecha() == null ? LocalDate.MAX : o.fecha()).reversed());
        return out;
    }

    @Transactional
    public Vista crear(Integer eventoId, Solicitud s, int usuarioId) {
        Evento ev = eventos.findById(eventoId).orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));
        if (viajes.findByEventoId(eventoId).isPresent()) throw new IllegalStateException("Ese evento ya tiene un viaje: abrelo para editarlo");
        ViajeCompetencia v = new ViajeCompetencia();
        v.setEventoId(eventoId);
        v.setCreadoPor(usuarioId);
        v.setFechaCreacion(ahora());
        aplicar(v, s, ev);
        return vista(viajes.save(v).getId());
    }

    @Transactional
    public Vista actualizar(Integer viajeId, Solicitud s) {
        ViajeCompetencia v = viaje(viajeId);
        exigirAbierto(v);
        Evento ev = eventos.findById(v.getEventoId()).orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));
        aplicar(v, s, ev);
        viajes.save(v);
        return vista(viajeId);
    }

    public Vista vista(Integer viajeId) {
        ViajeCompetencia v = viaje(viajeId);
        Evento ev = eventos.findById(v.getEventoId()).orElse(null);
        Map<Integer, Deportista> deps = new HashMap<>();
        for (Deportista d : deportistas.findAll()) deps.put(d.getId(), d);
        BigDecimal costo = v.getCostoPorPersona() == null ? BigDecimal.ZERO : v.getCostoPorPersona();

        List<Participante> filas = new ArrayList<>();
        int dep = 0, acomp = 0, conf = 0, pend = 0, no = 0, sinAut = 0;
        BigDecimal costoTotal = BigDecimal.ZERO, abonadoTotal = BigDecimal.ZERO;
        for (ViajeParticipante p : participantes.findByViajeIdOrderByIdAsc(viajeId)) {
            Deportista d = p.getDeportistaId() == null ? null : deps.get(p.getDeportistaId());
            Integer edad = edad(d, v.getFechaSalida());
            boolean menor = p.getTipo() == Tipo.DEPORTISTA && (edad == null || edad < MAYORIA_EDAD);
            BigDecimal abonado = abonos.findByParticipanteIdOrderByIdAsc(p.getId()).stream().map(ViajeAbono::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
            boolean va = p.getEstado() != ViajeParticipante.Estado.NO_VA;
            BigDecimal aPagar = va ? costo : BigDecimal.ZERO;
            filas.add(new Participante(p.getId(), p.getTipo(), p.getDeportistaId(), nombre(p, d), p.getAcompananteDocumento(), p.getAcompananteRol(),
                    edad, menor, p.getEstado(), p.isAutorizacionViaje(), p.getFechaAutorizacion(), p.getNotas(), aPagar, abonado, aPagar.subtract(abonado)));
            if (p.getTipo() == Tipo.DEPORTISTA) dep++; else acomp++;
            switch (p.getEstado()) {
                case CONFIRMADO -> conf++;
                case PENDIENTE -> pend++;
                case NO_VA -> no++;
            }
            if (va) {
                costoTotal = costoTotal.add(aPagar);
                abonadoTotal = abonadoTotal.add(abonado);
            }
            if (menor && va && !p.isAutorizacionViaje()) sinAut++;
        }
        Resumen r = new Resumen(dep, acomp, conf, pend, no, sinAut, costoTotal, abonadoTotal, costoTotal.subtract(abonadoTotal));
        return new Vista(v.getId(), v.getEventoId(), ev == null ? "(evento eliminado)" : ev.getNombre(), ev == null ? null : ev.getFecha(),
                v.getDestino(), v.getFechaSalida(), v.getFechaRegreso(), v.getPuntoEncuentro(), v.getTransporte(), v.getAlojamiento(),
                v.getCostoPorPersona(), v.getNotas(), v.getEstado(), r, filas);
    }

    /** Agrega a la delegacion los deportistas (titulares y suplentes) de todos los rosters del evento. @return cuantos agrego */
    @Transactional
    public int cargarDelegacion(Integer viajeId) {
        ViajeCompetencia v = viaje(viajeId);
        exigirAbierto(v);
        Set<Integer> ya = new HashSet<>();
        for (ViajeParticipante p : participantes.findByViajeIdOrderByIdAsc(viajeId)) if (p.getDeportistaId() != null) ya.add(p.getDeportistaId());
        int n = 0;
        for (RosterCompetencia r : rosters.findAll()) {
            if (!r.getEventoId().equals(v.getEventoId())) continue;
            for (RosterIntegrante i : integrantes.findByRosterIdOrderByRolAscIdAsc(r.getId())) {
                if (!ya.add(i.getDeportistaId())) continue;
                ViajeParticipante p = new ViajeParticipante();
                p.setViajeId(viajeId);
                p.setTipo(Tipo.DEPORTISTA);
                p.setDeportistaId(i.getDeportistaId());
                participantes.save(p);
                n++;
            }
        }
        if (n == 0 && ya.isEmpty()) throw new IllegalStateException("El evento aun no tiene rosters con deportistas (arma el roster en C2 primero)");
        return n;
    }

    @Transactional
    public Vista agregarAcompanante(Integer viajeId, String nombre, String documento, RolAcompanante rol) {
        ViajeCompetencia v = viaje(viajeId);
        exigirAbierto(v);
        String n = texto(nombre, 2, 120, "El nombre");
        if (rol == null) throw new IllegalArgumentException("Indica si es entrenador, acudiente u otro");
        ViajeParticipante p = new ViajeParticipante();
        p.setViajeId(viajeId);
        p.setTipo(Tipo.ACOMPANANTE);
        p.setAcompananteNombre(n);
        p.setAcompananteDocumento(opcional(documento, 30));
        p.setAcompananteRol(rol);
        participantes.save(p);
        return vista(viajeId);
    }

    @Transactional
    public Vista actualizarParticipante(Integer id, ViajeParticipante.Estado estado, Boolean autorizacion, String notas) {
        ViajeParticipante p = participante(id);
        ViajeCompetencia v = viaje(p.getViajeId());
        exigirAbierto(v);
        boolean aut = autorizacion == null ? p.isAutorizacionViaje() : autorizacion;
        ViajeParticipante.Estado nuevo = estado == null ? p.getEstado() : estado;
        if (p.getTipo() == Tipo.ACOMPANANTE && autorizacion != null && autorizacion) {
            throw new IllegalArgumentException("La autorizacion de viaje aplica solo a deportistas");
        }
        if (nuevo == ViajeParticipante.Estado.CONFIRMADO && p.getTipo() == Tipo.DEPORTISTA) {
            Deportista d = deportistas.findById(p.getDeportistaId()).orElse(null);
            Integer edad = edad(d, v.getFechaSalida());
            if ((edad == null || edad < MAYORIA_EDAD) && !aut) {
                throw new IllegalArgumentException("Es menor de edad (o sin fecha de nacimiento): registra primero la autorizacion de viaje firmada por el acudiente");
            }
        }
        if (autorizacion != null && autorizacion != p.isAutorizacionViaje()) {
            if (!autorizacion && p.getEstado() == ViajeParticipante.Estado.CONFIRMADO && nuevo == ViajeParticipante.Estado.CONFIRMADO) {
                throw new IllegalArgumentException("Esta confirmado: primero pasalo a pendiente para retirar la autorizacion");
            }
            p.setAutorizacionViaje(autorizacion);
            p.setFechaAutorizacion(autorizacion ? hoy() : null);
        }
        p.setEstado(nuevo);
        if (notas != null) p.setNotas(opcional(notas, 300));
        participantes.save(p);
        return vista(p.getViajeId());
    }

    @Transactional
    public Vista quitarParticipante(Integer id) {
        ViajeParticipante p = participante(id);
        exigirAbierto(viaje(p.getViajeId()));
        if (!abonos.findByParticipanteIdOrderByIdAsc(id).isEmpty()) {
            throw new IllegalStateException("Ya tiene abonos: marcalo como \"No va\" en vez de quitarlo");
        }
        participantes.deleteById(id);
        return vista(p.getViajeId());
    }

    @Transactional
    public Vista abonar(Integer participanteId, BigDecimal monto, String nota, int usuarioId) {
        ViajeParticipante p = participante(participanteId);
        ViajeCompetencia v = viaje(p.getViajeId());
        if (p.getEstado() == ViajeParticipante.Estado.NO_VA) throw new IllegalStateException("Este participante esta marcado como \"No va\"");
        if (v.getCostoPorPersona() == null || v.getCostoPorPersona().signum() <= 0) throw new IllegalStateException("Define primero el costo por persona del viaje");
        if (monto == null || monto.signum() <= 0) throw new IllegalArgumentException("El abono debe ser mayor a cero");
        BigDecimal abonado = abonos.findByParticipanteIdOrderByIdAsc(participanteId).stream().map(ViajeAbono::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldo = v.getCostoPorPersona().subtract(abonado);
        if (monto.compareTo(saldo) > 0) throw new IllegalArgumentException("El abono supera el saldo pendiente (" + saldo.toPlainString() + ")");
        ViajeAbono a = new ViajeAbono();
        a.setParticipanteId(participanteId);
        a.setMonto(monto);
        a.setFecha(hoy());
        a.setNota(opcional(nota, 200));
        a.setRegistradoPor(usuarioId);
        abonos.save(a);
        return vista(p.getViajeId());
    }

    @Transactional
    public Vista cerrar(Integer viajeId) {
        ViajeCompetencia v = viaje(viajeId);
        exigirAbierto(v);
        Vista vista = vista(viajeId);
        if (vista.resumen().pendientes() > 0) throw new IllegalArgumentException("Hay " + vista.resumen().pendientes() + " participante(s) pendientes: confirma o marca que no van");
        long confirmados = vista.participantes().stream().filter(p -> p.estado() == ViajeParticipante.Estado.CONFIRMADO).count();
        if (confirmados == 0) throw new IllegalArgumentException("No hay participantes confirmados");
        boolean hayMenores = vista.participantes().stream().anyMatch(p -> p.menor() && p.estado() == ViajeParticipante.Estado.CONFIRMADO);
        boolean hayAcompanante = vista.participantes().stream().anyMatch(p -> p.tipo() == Tipo.ACOMPANANTE && p.estado() == ViajeParticipante.Estado.CONFIRMADO);
        if (hayMenores && !hayAcompanante) throw new IllegalArgumentException("Viajan menores de edad: confirma al menos un acompanante adulto");
        if (vista.resumen().menoresSinAutorizacion() > 0) throw new IllegalArgumentException("Faltan autorizaciones de viaje de menores");
        v.setEstado(Estado.CERRADO);
        viajes.save(v);
        return vista(viajeId);
    }

    @Transactional
    public Vista reabrir(Integer viajeId) {
        ViajeCompetencia v = viaje(viajeId);
        if (v.getEstado() != Estado.CERRADO) throw new IllegalStateException("El viaje ya esta abierto");
        v.setEstado(Estado.ABIERTO);
        viajes.save(v);
        return vista(viajeId);
    }

    // ---------------- Internos ----------------

    private void aplicar(ViajeCompetencia v, Solicitud s, Evento ev) {
        if (s == null) throw new IllegalArgumentException("Faltan los datos del viaje");
        String destino = texto(s.destino(), 2, 150, "El destino");
        if (s.fechaSalida() == null || s.fechaRegreso() == null) throw new IllegalArgumentException("Indica la fecha de salida y de regreso");
        if (s.fechaRegreso().isBefore(s.fechaSalida())) throw new IllegalArgumentException("El regreso no puede ser antes de la salida");
        if (ev.getFecha() != null && (ev.getFecha().isBefore(s.fechaSalida()) || ev.getFecha().isAfter(s.fechaRegreso()))) {
            throw new IllegalArgumentException("La fecha del evento (" + ev.getFecha() + ") debe caer dentro del viaje");
        }
        if (s.costoPorPersona() != null && s.costoPorPersona().signum() < 0) throw new IllegalArgumentException("El costo no puede ser negativo");
        v.setDestino(destino);
        v.setFechaSalida(s.fechaSalida());
        v.setFechaRegreso(s.fechaRegreso());
        v.setPuntoEncuentro(opcional(s.puntoEncuentro(), 150));
        v.setTransporte(opcional(s.transporte(), 150));
        v.setAlojamiento(opcional(s.alojamiento(), 200));
        v.setCostoPorPersona(s.costoPorPersona());
        v.setNotas(opcional(s.notas(), 500));
    }

    private ViajeCompetencia viaje(Integer id) {
        return viajes.findById(id).orElseThrow(() -> new IllegalArgumentException("Viaje no encontrado"));
    }

    private ViajeParticipante participante(Integer id) {
        return participantes.findById(id).orElseThrow(() -> new IllegalArgumentException("Participante no encontrado"));
    }

    private static void exigirAbierto(ViajeCompetencia v) {
        if (v.getEstado() == Estado.CERRADO) throw new IllegalStateException("El viaje esta cerrado: reabrelo para editarlo");
    }

    private static Integer edad(Deportista d, LocalDate ref) {
        if (d == null || d.getPersona() == null || d.getPersona().getFechaNacimiento() == null) return null;
        return d.getPersona().getFechaNacimiento().until(ref).getYears();
    }

    private static String nombre(ViajeParticipante p, Deportista d) {
        if (p.getTipo() == Tipo.ACOMPANANTE) return p.getAcompananteNombre();
        if (d == null || d.getPersona() == null) return "(sin nombre)";
        return d.getPersona().getNombre() + " " + d.getPersona().getApellidos();
    }

    private static String texto(String s, int min, int max, String campo) {
        String t = s == null ? "" : s.replaceAll("[\\p{Cntrl}]", "").trim();
        if (t.length() < min) throw new IllegalArgumentException(campo + " es obligatorio (minimo " + min + " caracteres)");
        if (t.length() > max) throw new IllegalArgumentException(campo + " admite maximo " + max + " caracteres");
        return t;
    }

    private static String opcional(String s, int max) {
        if (s == null || s.isBlank()) return null;
        String t = s.replaceAll("[\\p{Cntrl}&&[^\\n]]", "").trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
