package com.varcal.cheermanager.Service.Importacion;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Funcionalidad.Auditoria;
import com.varcal.cheermanager.Service.Clubes.SuscripcionService;
import com.varcal.cheermanager.Service.Funcionalidad.AuditoriaService;
import com.varcal.cheermanager.Service.Reportes.ReportesService;
import com.varcal.cheermanager.tenant.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Exportes de listados en CSV (UTF-8 con BOM, separador ";": se abren directo en Excel).
 * Son consultas nativas filtradas a mano por club_id (las nativas no pasan por el filtro @TenantId).
 * Son de solo lectura, asi que siguen disponibles aunque la suscripcion del club este vencida.
 * No incluyen datos de salud ni de acceso (claves, usuarios).
 */
@Service
public class ExportesService {

    private static final Logger log = LoggerFactory.getLogger(ExportesService.class);

    private static final String SQL_DEPORTISTAS = "SELECT p.numero_documento, p.tipo_documento, p.nombre, p.apellidos, "
            + "p.fecha_nacimiento, g.genero, ep.estado, n.nombre, c.nombre_empresa, p.telefono, p.direccion, "
            + "d.contacto_emergencia, d.altura, d.peso, d.fecha_inscripcion, "
            + "a.nombre, a.apellidos, a.telefono, a.email, da.parentesco "
            + "FROM deportistas d "
            + "JOIN personas p ON p.id = d.persona_id "
            + "LEFT JOIN generos g ON g.id = p.genero_id "
            + "LEFT JOIN estados_persona ep ON ep.id = d.estado_id "
            + "LEFT JOIN niveles n ON n.id = d.nivel_actual_id AND n.club_id = d.club_id "
            + "LEFT JOIN convenios c ON c.id = d.convenio_id AND c.club_id = d.club_id "
            + "LEFT JOIN deportista_acudientes da ON da.deportista_id = d.id AND da.principal = 1 AND da.club_id = d.club_id "
            + "LEFT JOIN acudientes a ON a.id = da.acudiente_id AND a.club_id = d.club_id "
            + "WHERE d.club_id = :club ORDER BY p.apellidos, p.nombre";

    private static final String SQL_ACUDIENTES = "SELECT a.numero_documento, a.tipo_documento, a.nombre, a.apellidos, "
            + "a.telefono, a.email, a.direccion, CASE WHEN a.usuario_id IS NULL THEN 'No' ELSE 'Si' END, "
            + "GROUP_CONCAT(CONCAT(p.nombre, ' ', p.apellidos) ORDER BY p.apellidos SEPARATOR ' | ') "
            + "FROM acudientes a "
            + "LEFT JOIN deportista_acudientes da ON da.acudiente_id = a.id AND da.club_id = a.club_id "
            + "LEFT JOIN deportistas d ON d.id = da.deportista_id AND d.club_id = a.club_id "
            + "LEFT JOIN personas p ON p.id = d.persona_id "
            + "WHERE a.club_id = :club GROUP BY a.id ORDER BY a.apellidos, a.nombre";

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private AuditoriaService auditoriaService;

    public byte[] deportistas() {
        List<List<String>> filas = new ArrayList<>();
        filas.add(List.of("numero_documento", "tipo_documento", "nombre", "apellidos", "fecha_nacimiento", "edad",
                "genero", "estado", "nivel", "convenio", "telefono", "direccion", "contacto_emergencia", "altura",
                "peso", "fecha_inscripcion", "acudiente_nombre", "acudiente_apellidos", "acudiente_telefono",
                "acudiente_email", "acudiente_parentesco"));
        LocalDate hoy = SuscripcionService.hoy();
        for (Object[] r : consulta(SQL_DEPORTISTAS)) {
            LocalDate nac = fecha(r[4]);
            List<String> f = new ArrayList<>();
            f.add(txt(r[0]));
            f.add(txt(r[1]));
            f.add(txt(r[2]));
            f.add(txt(r[3]));
            f.add(nac == null ? "" : nac.toString());
            f.add(nac == null ? "" : String.valueOf(Period.between(nac, hoy).getYears()));
            for (int i = 5; i <= 13; i++) {
                f.add(txt(r[i]));
            }
            LocalDate insc = fecha(r[14]);
            f.add(insc == null ? "" : insc.toString());
            for (int i = 15; i <= 19; i++) {
                f.add(txt(r[i]));
            }
            filas.add(f);
        }
        registrar("deportistas", filas.size() - 1, null);
        return CsvUtil.escribir(filas);
    }

    public byte[] acudientes() {
        List<List<String>> filas = new ArrayList<>();
        filas.add(List.of("numero_documento", "tipo_documento", "nombre", "apellidos", "telefono", "email",
                "direccion", "tiene_acceso_portal", "deportistas"));
        for (Object[] r : consulta(SQL_ACUDIENTES)) {
            List<String> f = new ArrayList<>();
            for (Object o : r) {
                f.add(txt(o));
            }
            filas.add(f);
        }
        registrar("acudientes", filas.size() - 1, null);
        return CsvUtil.escribir(filas);
    }

    // ------------------------------------------------------------------ exportes por rango de fechas

    private static final String SQL_FACTURAS = "SELECT f.numero_factura, f.fecha_emision, f.fecha_vencimiento, "
            + "per.numero_documento, per.nombre, per.apellidos, COALESCE(f.concepto, ''), COALESCE(f.periodo, ''), "
            + "COALESCE(f.descripcion, ''), f.total, "
            + "COALESCE((SELECT SUM(p.monto) FROM pagos p WHERE p.factura_id = f.id AND p.estado = 'Pagado'), 0), "
            + "f.anulada, COALESCE(f.motivo_anulacion, '') "
            + "FROM facturas f JOIN personas per ON per.id = f.persona_id "
            + "WHERE f.club_id = :club AND f.fecha_emision BETWEEN :desde AND :hasta ORDER BY f.fecha_emision, f.id";

    private static final String SQL_PAGOS = "SELECT p.fecha, f.numero_factura, per.numero_documento, per.nombre, "
            + "per.apellidos, tp.tipo, mp.metodo, p.estado, p.monto, COALESCE(p.referencia, ''), "
            + "CASE WHEN p.conciliado_en IS NULL THEN 'No' ELSE 'Si' END, COALESCE(p.motivo_anulacion, '') "
            + "FROM pagos p JOIN facturas f ON f.id = p.factura_id JOIN personas per ON per.id = f.persona_id "
            + "JOIN tipos_pago tp ON tp.id = p.tipo_pago_id JOIN metodos_pago mp ON mp.id = p.metodo_pago_id "
            + "WHERE p.club_id = :club AND p.fecha BETWEEN :desde AND :hasta ORDER BY p.fecha, p.id";

    private static final String SQL_CAJA = "SELECT c.fecha, c.estado, c.revision, c.total_pagado, c.efectivo_esperado, "
            + "c.efectivo_contado, c.diferencia, COALESCE(c.nota, ''), c.cerrado_en, COALESCE(c.motivo_reapertura, '') "
            + "FROM cierres_caja c WHERE c.club_id = :club AND c.fecha BETWEEN :desde AND :hasta "
            + "ORDER BY c.fecha, c.revision";

    private static final String SQL_ASISTENCIA = "SELECT a.fecha, per.numero_documento, per.nombre, per.apellidos, "
            + "COALESCE(dia.dia, ''), h.hora_inicio, h.hora_fin, ea.estado "
            + "FROM asistencias a JOIN deportistas d ON d.id = a.deportista_id JOIN personas per ON per.id = d.persona_id "
            + "JOIN horarios h ON h.id = a.horario_id LEFT JOIN dias_semana dia ON dia.id = h.dia_id "
            + "JOIN estados_asistencia ea ON ea.id = a.estado_asistencia_id "
            + "WHERE a.club_id = :club AND a.fecha BETWEEN :desde AND :hasta ORDER BY a.fecha, per.apellidos, per.nombre";

    private static final String SQL_EVALUACIONES = "SELECT e.fecha, per.numero_documento, per.nombre, per.apellidos, "
            + "c.nombre, e.puntaje_obtenido "
            + "FROM evaluaciones e JOIN deportistas d ON d.id = e.deportista_id JOIN personas per ON per.id = d.persona_id "
            + "JOIN criterios_evaluacion c ON c.id = e.criterio_id "
            + "WHERE e.club_id = :club AND e.fecha BETWEEN :desde AND :hasta ORDER BY e.fecha, per.apellidos, per.nombre";

    public byte[] facturas(LocalDate desde, LocalDate hasta) {
        ReportesService.Rango r = ReportesService.rango(desde, hasta, SuscripcionService.hoy());
        LocalDate hoy = SuscripcionService.hoy();
        List<List<String>> filas = new ArrayList<>();
        filas.add(List.of("numero_factura", "fecha_emision", "fecha_vencimiento", "documento", "nombre", "apellidos",
                "concepto", "periodo", "descripcion", "total", "pagado", "saldo", "estado", "motivo_anulacion"));
        for (Object[] x : consultaRango(SQL_FACTURAS, r)) {
            BigDecimal total = dec(x[9]);
            BigDecimal pagado = dec(x[10]);
            BigDecimal saldo = total.subtract(pagado);
            LocalDate venc = fecha(x[2]);
            String estado;
            if (bool(x[11])) {
                estado = "Anulada";
                saldo = BigDecimal.ZERO;
            } else if (saldo.signum() <= 0) {
                estado = "Pagada";
            } else if (venc != null && venc.isBefore(hoy)) {
                estado = "En mora";
            } else if (pagado.signum() > 0) {
                estado = "Abono parcial";
            } else {
                estado = "Pendiente";
            }
            filas.add(List.of(txt(x[0]), txt(fecha(x[1])), txt(venc), txt(x[3]), txt(x[4]), txt(x[5]), txt(x[6]),
                    txt(x[7]), txt(x[8]), num(total), num(pagado), num(saldo), estado, txt(x[12])));
        }
        registrar("facturas", filas.size() - 1, r);
        return CsvUtil.escribir(filas);
    }

    public byte[] pagos(LocalDate desde, LocalDate hasta) {
        return simple("pagos", SQL_PAGOS, desde, hasta,
                List.of("fecha", "numero_factura", "documento", "nombre", "apellidos", "tipo_pago", "metodo_pago",
                        "estado", "monto", "referencia", "conciliado", "motivo_anulacion"));
    }

    public byte[] cierresCaja(LocalDate desde, LocalDate hasta) {
        return simple("cierres_caja", SQL_CAJA, desde, hasta,
                List.of("fecha", "estado", "revision", "total_pagado", "efectivo_esperado", "efectivo_contado",
                        "diferencia", "nota", "cerrado_en", "motivo_reapertura"));
    }

    public byte[] asistencia(LocalDate desde, LocalDate hasta) {
        return simple("asistencia", SQL_ASISTENCIA, desde, hasta,
                List.of("fecha", "documento", "nombre", "apellidos", "dia", "hora_inicio", "hora_fin", "estado"));
    }

    public byte[] evaluaciones(LocalDate desde, LocalDate hasta) {
        return simple("evaluaciones", SQL_EVALUACIONES, desde, hasta,
                List.of("fecha", "documento", "nombre", "apellidos", "criterio", "puntaje"));
    }

    private byte[] simple(String que, String sql, LocalDate desde, LocalDate hasta, List<String> encabezado) {
        ReportesService.Rango r = ReportesService.rango(desde, hasta, SuscripcionService.hoy());
        List<List<String>> filas = new ArrayList<>();
        filas.add(encabezado);
        for (Object[] x : consultaRango(sql, r)) {
            List<String> f = new ArrayList<>();
            for (Object o : x) {
                f.add(o instanceof BigDecimal b ? num(b) : txt(o));
            }
            filas.add(f);
        }
        registrar(que, filas.size() - 1, r);
        return CsvUtil.escribir(filas);
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> consultaRango(String sql, ReportesService.Rango r) {
        return em.createNativeQuery(sql).setParameter("club", TenantContext.require())
                .setParameter("desde", Date.valueOf(r.desde())).setParameter("hasta", Date.valueOf(r.hasta()))
                .getResultList();
    }

    private static BigDecimal dec(Object o) {
        return o == null ? BigDecimal.ZERO : o instanceof BigDecimal b ? b : new BigDecimal(o.toString());
    }

    /** Monto con punto decimal y 2 decimales, sin separador de miles: Excel lo reconoce como numero. */
    private static String num(BigDecimal b) {
        return b.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static boolean bool(Object o) {
        return o instanceof Boolean b ? b : o instanceof Number n && n.intValue() != 0;
    }

    private static String txt(LocalDate d) {
        return d == null ? "" : d.toString();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> consulta(String sql) {
        return em.createNativeQuery(sql).setParameter("club", TenantContext.require()).getResultList();
    }

    private static String txt(Object o) {
        return o == null ? "" : o.toString();
    }

    private static LocalDate fecha(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof LocalDate d) {
            return d;
        }
        if (o instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        return LocalDate.parse(o.toString().substring(0, 10));
    }

    /** Deja constancia en el log y en la auditoria: quien descargo que, cuantas filas y con que rango. */
    private void registrar(String que, int filas, ReportesService.Rango rango) {
        log.info("EXPORTE {} club={} filas={}", que, TenantContext.require(), filas);
        String detalle = "{\"recurso\":\"" + que + "\",\"filas\":" + filas
                + (rango == null ? "" : ",\"desde\":\"" + rango.desde() + "\",\"hasta\":\"" + rango.hasta() + "\"")
                + "}";
        auditoriaService.registrarEvento("exportes", Auditoria.AccionEnum.EXPORTE, null, null, detalle);
    }
}
