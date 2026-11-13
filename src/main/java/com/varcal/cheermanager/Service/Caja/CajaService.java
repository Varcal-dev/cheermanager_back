package com.varcal.cheermanager.Service.Caja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.varcal.cheermanager.Models.Financiero.CierreCaja;
import com.varcal.cheermanager.Service.Financiero.FacturaService;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Financiero.CierreCajaRepository;
import com.varcal.cheermanager.repository.Financiero.PagoRepository;

/**
 * Caja diaria: resumen de lo recaudado, cierre con conteo de efectivo y reapertura con motivo.
 * Todo pago pasa por PagoService, que llama a exigirDiaAbierto antes de registrar, confirmar o anular un pago
 * "Pagado": un dia CERRADO queda protegido hasta que alguien con permiso lo reabra.
 */
@Service
public class CajaService {

    public static final String ABIERTA = "ABIERTA";
    private static final DateTimeFormatter FMT = CajaReglas.FMT;
    private static final ObjectMapper JSON = new ObjectMapper();

    public record Linea(String nombre, long cantidad, BigDecimal total) {
    }

    public record Resumen(LocalDate fecha, String estado, Integer revision,
            BigDecimal totalPagado, BigDecimal efectivoEsperado, List<Linea> porMetodo, List<Linea> porTipo,
            long pendientes, BigDecimal totalPendiente, long anulados, BigDecimal totalAnulado,
            BigDecimal efectivoContado, BigDecimal diferencia, String nota, String cerradoPor, LocalDateTime cerradoEn,
            String reabiertoPor, LocalDateTime reabiertoEn, String motivoReapertura, boolean desfaseConCierre) {
    }

    public record CierreResumen(LocalDate fecha, String estado, Integer revision, BigDecimal totalPagado,
            BigDecimal efectivoEsperado, BigDecimal efectivoContado, BigDecimal diferencia, String nota,
            String cerradoPor, LocalDateTime cerradoEn, String reabiertoPor, LocalDateTime reabiertoEn,
            String motivoReapertura) {
    }

    private final PagoRepository pagos;
    private final CierreCajaRepository cierres;
    private final UserRepository usuarios;

    public CajaService(PagoRepository pagos, CierreCajaRepository cierres, UserRepository usuarios) {
        this.pagos = pagos;
        this.cierres = cierres;
        this.usuarios = usuarios;
    }

    // ------------------------------------------------------------------ guardia usada por PagoService

    /** Lanza si la caja de esa fecha esta cerrada. Sin cierre, o REABIERTA, deja pasar. */
    @Transactional(readOnly = true)
    public void exigirDiaAbierto(LocalDate fecha) {
        if (fecha == null) {
            return;
        }
        cierres.findByFecha(fecha).ifPresent(c -> {
            if (CierreCaja.CERRADA.equals(c.getEstado())) {
                throw new RuntimeException("La caja del " + fecha.format(FMT)
                        + " esta cerrada: reabrela (con motivo) para registrar, confirmar o anular pagos de esa fecha");
            }
        });
    }

    // ------------------------------------------------------------------ consultas

    private static BigDecimal dec(Object o) {
        return o instanceof BigDecimal b ? b : new BigDecimal(String.valueOf(o));
    }

    private static List<Linea> lineas(List<Object[]> filas) {
        List<Linea> out = new ArrayList<>();
        for (Object[] f : filas) {
            out.add(new Linea(String.valueOf(f[0]), ((Number) f[1]).longValue(), dec(f[2])));
        }
        return out;
    }

    private static BigDecimal suma(List<Linea> l) {
        return l.stream().map(Linea::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal efectivo(List<Linea> porMetodo) {
        return porMetodo.stream().filter(l -> "efectivo".equalsIgnoreCase(l.nombre().trim()))
                .map(Linea::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String nombreUsuario(Integer id) {
        if (id == null) {
            return null;
        }
        return usuarios.findById(id.longValue()).map(u -> u.getUsername()).orElse("usuario #" + id);
    }

    @Transactional(readOnly = true)
    public Resumen resumen(LocalDate fecha) {
        if (fecha == null) {
            throw new RuntimeException("La fecha es obligatoria");
        }
        List<Linea> porMetodo = lineas(pagos.resumenPagadoPorMetodo(fecha));
        List<Linea> porTipo = lineas(pagos.resumenPagadoPorTipo(fecha));
        BigDecimal total = suma(porMetodo);
        BigDecimal esperado = efectivo(porMetodo);
        Object[] pend = pagos.resumenPendientes(fecha).get(0);
        Object[] anul = pagos.resumenAnulados(fecha).get(0);
        CierreCaja c = cierres.findByFecha(fecha).orElse(null);
        String estado = c == null ? ABIERTA : c.getEstado();
        boolean desfase = c != null && CierreCaja.CERRADA.equals(c.getEstado())
                && (c.getTotalPagado().compareTo(total) != 0 || c.getEfectivoEsperado().compareTo(esperado) != 0);
        return new Resumen(fecha, estado, c == null ? null : c.getRevision(), total, esperado, porMetodo, porTipo,
                ((Number) pend[0]).longValue(), dec(pend[1]), ((Number) anul[0]).longValue(), dec(anul[1]),
                c == null ? null : c.getEfectivoContado(), c == null ? null : c.getDiferencia(),
                c == null ? null : c.getNota(),
                c == null ? null : nombreUsuario(c.getCerradoPorUsuarioId()), c == null ? null : c.getCerradoEn(),
                c == null ? null : nombreUsuario(c.getReabiertoPorUsuarioId()), c == null ? null : c.getReabiertoEn(),
                c == null ? null : c.getMotivoReapertura(), desfase);
    }

    @Transactional(readOnly = true)
    public List<CierreResumen> listarCierres(LocalDate desde, LocalDate hasta) {
        LocalDate h = hasta == null ? com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy() : hasta;
        LocalDate d = desde == null ? h.minusDays(60) : desde;
        if (d.isAfter(h)) {
            throw new RuntimeException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        if (d.plusDays(366).isBefore(h)) {
            throw new RuntimeException("El rango no puede pasar de un año");
        }
        List<CierreResumen> out = new ArrayList<>();
        for (CierreCaja c : cierres.findByFechaBetweenOrderByFechaDesc(d, h)) {
            out.add(new CierreResumen(c.getFecha(), c.getEstado(), c.getRevision(), c.getTotalPagado(),
                    c.getEfectivoEsperado(), c.getEfectivoContado(), c.getDiferencia(), c.getNota(),
                    nombreUsuario(c.getCerradoPorUsuarioId()), c.getCerradoEn(),
                    nombreUsuario(c.getReabiertoPorUsuarioId()), c.getReabiertoEn(), c.getMotivoReapertura()));
        }
        return out;
    }

    // ------------------------------------------------------------------ cerrar / reabrir

    @Transactional
    public Resumen cerrar(LocalDate fecha, BigDecimal efectivoContado, String nota) {
        Resumen vivo = fecha == null ? null : resumen(fecha);
        CierreCaja c = fecha == null ? null : cierres.findByFecha(fecha).orElse(null);
        String n = CajaReglas.validarCierre(fecha, com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy(), efectivoContado, vivo == null ? BigDecimal.ZERO : vivo.efectivoEsperado(),
                nota, c == null ? null : c.getEstado());
        if (c == null) {
            c = new CierreCaja();
            c.setFecha(fecha);
            c.setRevision(1);
        } else {
            c.setRevision((c.getRevision() == null ? 1 : c.getRevision()) + 1);
        }
        c.setEstado(CierreCaja.CERRADA);
        c.setTotalPagado(vivo.totalPagado());
        c.setEfectivoEsperado(vivo.efectivoEsperado());
        c.setEfectivoContado(efectivoContado);
        c.setDiferencia(CajaReglas.diferencia(vivo.efectivoEsperado(), efectivoContado));
        c.setNota(n);
        c.setDetalleJson(foto(vivo));
        c.setCerradoPorUsuarioId(FacturaService.usuarioActualId());
        c.setCerradoEn(LocalDateTime.now());
        cierres.save(c);
        return resumen(fecha);
    }

    @Transactional
    public Resumen reabrir(LocalDate fecha, String motivo) {
        String m = FacturaService.validarMotivo(motivo);
        if (fecha == null) {
            throw new RuntimeException("La fecha es obligatoria");
        }
        CierreCaja c = cierres.findByFecha(fecha)
                .orElseThrow(() -> new RuntimeException("La caja del " + fecha.format(FMT) + " no tiene cierre"));
        if (!CierreCaja.CERRADA.equals(c.getEstado())) {
            throw new RuntimeException("La caja del " + fecha.format(FMT) + " ya esta abierta");
        }
        c.setEstado(CierreCaja.REABIERTA);
        c.setReabiertoPorUsuarioId(FacturaService.usuarioActualId());
        c.setReabiertoEn(LocalDateTime.now());
        c.setMotivoReapertura(m);
        cierres.save(c);
        return resumen(fecha);
    }

    /** Foto del recaudo (por metodo y por tipo). Cabe en 4000 caracteres: si no, se guarda solo lo esencial. */
    static String foto(Resumen r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("porMetodo", r.porMetodo());
        m.put("porTipo", r.porTipo());
        m.put("pendientes", r.pendientes());
        m.put("anulados", r.anulados());
        try {
            String s = JSON.writeValueAsString(m);
            if (s.length() <= 4000) {
                return s;
            }
            m.remove("porTipo");
            s = JSON.writeValueAsString(m);
            return s.length() <= 4000 ? s : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /** Lee de vuelta una foto (para pruebas y futuras pantallas). */
    static Map<String, Object> leerFoto(String json) {
        try {
            return JSON.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}
