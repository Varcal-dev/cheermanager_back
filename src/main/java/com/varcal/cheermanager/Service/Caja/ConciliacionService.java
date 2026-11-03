package com.varcal.cheermanager.Service.Caja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Financiero.EstadoPago;
import com.varcal.cheermanager.Models.Financiero.Pago;
import com.varcal.cheermanager.Service.Financiero.FacturaService;
import com.varcal.cheermanager.Service.Importacion.CsvUtil;
import com.varcal.cheermanager.repository.Financiero.PagoRepository;

/**
 * Conciliacion: marcar los pagos que NO son en efectivo (transferencia, Nequi, tarjeta...) como verificados contra
 * el extracto del banco. Conciliar no cambia montos ni estados: solo deja constancia de quien verifico y cuando,
 * por eso funciona aunque la caja de ese dia este cerrada.
 */
@Service
public class ConciliacionService {

    public static final int MAX_FILAS_EXTRACTO = 5000;
    public static final int MAX_BYTES_EXTRACTO = 2 * 1024 * 1024;

    public record PagoFila(Integer id, LocalDate fecha, BigDecimal monto, String metodo, String tipo,
            String numeroFactura, String referencia, boolean conciliado, LocalDateTime conciliadoEn) {
    }

    public record Sugerencia(int fila, LocalDate fecha, BigDecimal valor, String descripcion, String referencia,
            Integer pagoId, String criterio, int diasDiferencia) {
    }

    public record LineaSinPago(int fila, LocalDate fecha, BigDecimal valor, String descripcion, String referencia) {
    }

    public record InformeExtracto(int lineasLeidas, int lineasIgnoradas, List<String> avisos,
            List<Sugerencia> sugerencias, List<LineaSinPago> lineasSinPago, List<PagoFila> pagosSinLinea) {
    }

    private final PagoRepository pagos;

    public ConciliacionService(PagoRepository pagos) {
        this.pagos = pagos;
    }

    private static PagoFila fila(Pago p) {
        return new PagoFila(p.getId(), p.getFecha(), p.getMonto(), p.getMetodoPago().getMetodo(),
                p.getTipoPago().getTipo(), p.getFactura().getNumeroFactura(), p.getReferencia(),
                p.getConciliadoEn() != null, p.getConciliadoEn());
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new RuntimeException("Indica el rango de fechas (desde y hasta)");
        }
        if (desde.isAfter(hasta)) {
            throw new RuntimeException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        if (desde.plusDays(366).isBefore(hasta)) {
            throw new RuntimeException("El rango no puede pasar de un año");
        }
    }

    /** soloPendientes: true = sin conciliar, false = conciliados, null = todos. */
    @Transactional(readOnly = true)
    public List<PagoFila> listar(LocalDate desde, LocalDate hasta, Boolean conciliado) {
        validarRango(desde, hasta);
        List<PagoFila> out = new ArrayList<>();
        for (Pago p : pagos.findNoEfectivoPagadosEnRango(desde, hasta)) {
            boolean c = p.getConciliadoEn() != null;
            if (conciliado == null || conciliado == c) {
                out.add(fila(p));
            }
        }
        return out;
    }

    @Transactional
    public int marcar(List<Integer> ids, boolean conciliado) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("Selecciona al menos un pago");
        }
        if (ids.size() > 500) {
            throw new RuntimeException("Maximo 500 pagos por operacion");
        }
        int n = 0;
        Integer usuario = FacturaService.usuarioActualId();
        LocalDateTime ahora = LocalDateTime.now();
        for (Integer id : new LinkedHashSet<>(ids)) {
            Pago p = pagos.findById(id).orElseThrow(() -> new RuntimeException("Pago no encontrado con ID: " + id));
            if (p.getEstado() != EstadoPago.Pagado) {
                throw new RuntimeException("El pago " + id + " no esta en estado Pagado: no se puede conciliar");
            }
            if ("efectivo".equalsIgnoreCase(p.getMetodoPago().getMetodo().trim())) {
                throw new RuntimeException("El pago " + id + " es en efectivo: se verifica en el cierre de caja");
            }
            if (conciliado) {
                if (p.getConciliadoEn() == null) {
                    p.setConciliadoEn(ahora);
                    p.setConciliadoPorUsuarioId(usuario);
                    pagos.save(p);
                    n++;
                }
            } else if (p.getConciliadoEn() != null) {
                p.setConciliadoEn(null);
                p.setConciliadoPorUsuarioId(null);
                pagos.save(p);
                n++;
            }
        }
        return n;
    }

    @Transactional
    public PagoFila actualizarReferencia(Integer id, String referencia) {
        Pago p = pagos.findById(id).orElseThrow(() -> new RuntimeException("Pago no encontrado con ID: " + id));
        String r = referencia == null ? "" : referencia.trim();
        if (r.length() > 100) {
            throw new RuntimeException("La referencia no puede pasar de 100 caracteres");
        }
        p.setReferencia(r.isEmpty() ? null : r);
        return fila(pagos.save(p));
    }

    /** Lee el extracto y sugiere cruces. No guarda nada: el usuario confirma despues con marcar(). */
    @Transactional(readOnly = true)
    public InformeExtracto analizarExtracto(byte[] csv, int ventanaDias) {
        if (csv == null || csv.length == 0) {
            throw new RuntimeException("El archivo esta vacio");
        }
        if (csv.length > MAX_BYTES_EXTRACTO) {
            throw new RuntimeException("El archivo pesa mas de 2 MB");
        }
        int ventana = Math.max(0, Math.min(ventanaDias, 10));
        List<String[]> filas = CsvUtil.leer(csv);
        if (filas.isEmpty()) {
            throw new RuntimeException("El archivo no tiene filas");
        }
        Map<String, Integer> col = new HashMap<>();
        String[] enc = filas.get(0);
        for (int i = 0; i < enc.length; i++) {
            col.putIfAbsent(ExtractoParseo.nombreColumna(enc[i]), i);
        }
        if (!col.containsKey("fecha") || !col.containsKey("valor")) {
            throw new RuntimeException("El extracto debe tener al menos las columnas 'fecha' y 'valor' "
                    + "(tambien se leen 'descripcion' y 'referencia')");
        }
        if (filas.size() - 1 > MAX_FILAS_EXTRACTO) {
            throw new RuntimeException("El extracto tiene mas de " + MAX_FILAS_EXTRACTO + " filas: dividelo por periodos");
        }
        List<String> avisos = new ArrayList<>();
        List<ConciliacionMatcher.Linea> lineas = new ArrayList<>();
        Map<Integer, String[]> textos = new HashMap<>(); // indice -> {descripcion, referencia}
        int ignoradas = 0;
        for (int i = 1; i < filas.size(); i++) {
            String[] f = filas.get(i);
            int numero = i + 1; // numero de fila tal como la ve la persona en la hoja
            if (vacia(f)) {
                continue;
            }
            try {
                LocalDate fecha = ExtractoParseo.fecha(celda(f, col.get("fecha")));
                BigDecimal valor = ExtractoParseo.monto(celda(f, col.get("valor")));
                if (valor.signum() <= 0) {
                    ignoradas++; // egresos y ceros: no son recaudo
                    continue;
                }
                String desc = celda(f, col.get("descripcion"));
                String ref = celda(f, col.get("referencia"));
                lineas.add(new ConciliacionMatcher.Linea(numero, fecha, valor, ref));
                textos.put(numero, new String[] { desc, ref });
            } catch (IllegalArgumentException e) {
                ignoradas++;
                if (avisos.size() < 20) {
                    avisos.add("Fila " + numero + ": " + e.getMessage() + " (se omitio)");
                }
            }
        }
        if (lineas.isEmpty()) {
            return new InformeExtracto(0, ignoradas, avisos, List.of(), List.of(), List.of());
        }
        LocalDate min = lineas.stream().map(ConciliacionMatcher.Linea::fecha).min(LocalDate::compareTo).get();
        LocalDate max = lineas.stream().map(ConciliacionMatcher.Linea::fecha).max(LocalDate::compareTo).get();
        // Pagos sin conciliar que podrian cruzar (se amplia el rango por la ventana de dias)
        List<Pago> posibles = new ArrayList<>();
        for (Pago p : pagos.findNoEfectivoPagadosEnRango(min.minusDays(ventana), max.plusDays(ventana))) {
            if (p.getConciliadoEn() == null) {
                posibles.add(p);
            }
        }
        Map<Integer, Pago> porId = new HashMap<>();
        List<ConciliacionMatcher.Candidato> candidatos = new ArrayList<>();
        for (Pago p : posibles) {
            porId.put(p.getId(), p);
            candidatos.add(new ConciliacionMatcher.Candidato(p.getId(), p.getFecha(), p.getMonto(), p.getReferencia()));
        }
        ConciliacionMatcher.Resultado r = ConciliacionMatcher.cruzar(lineas, candidatos, ventana);
        Map<Integer, ConciliacionMatcher.Linea> porLinea = new HashMap<>();
        lineas.forEach(l -> porLinea.put(l.indice(), l));

        List<Sugerencia> sugerencias = new ArrayList<>();
        for (ConciliacionMatcher.Coincidencia c : r.coincidencias()) {
            ConciliacionMatcher.Linea l = porLinea.get(c.indiceLinea());
            String[] t = textos.get(l.indice());
            sugerencias.add(new Sugerencia(l.indice(), l.fecha(), l.monto(), t[0], t[1], c.pagoId(), c.criterio(),
                    c.diasDiferencia()));
        }
        List<LineaSinPago> sinPago = new ArrayList<>();
        for (Integer idx : r.lineasSinPago()) {
            ConciliacionMatcher.Linea l = porLinea.get(idx);
            String[] t = textos.get(idx);
            sinPago.add(new LineaSinPago(idx, l.fecha(), l.monto(), t[0], t[1]));
        }
        List<PagoFila> sinLinea = new ArrayList<>();
        for (Integer id : r.pagosSinLinea()) {
            sinLinea.add(fila(porId.get(id)));
        }
        return new InformeExtracto(lineas.size(), ignoradas, avisos, sugerencias, sinPago, sinLinea);
    }

    private static boolean vacia(String[] f) {
        for (String s : f) {
            if (s != null && !s.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String celda(String[] f, Integer i) {
        if (i == null || i >= f.length || f[i] == null) {
            return "";
        }
        return f[i].trim();
    }
}
