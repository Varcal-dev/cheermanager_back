package com.varcal.cheermanager.Service.Clubes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pago en linea de la suscripcion del club a VarCal con Wompi (PAG-1).
 *
 * Flujo: el administrador del club pide pagar N meses -> se guarda una transaccion CREADA y se devuelve la URL del
 * Web Checkout de Wompi firmada (firma de integridad) -> Wompi avisa por webhook (transaction.updated) -> si el evento
 * trae un checksum valido y el pago esta APPROVED por el monto esperado, se registra el pago de la suscripcion y se
 * extiende la vigencia (SuscripcionService.registrarPago). El regreso del navegador a la pagina es solo informativo.
 *
 * Variables de entorno del servidor (nunca en el repo): WOMPI_PUBLIC_KEY, WOMPI_INTEGRITY_SECRET, WOMPI_EVENTS_SECRET
 * y, opcional, WOMPI_REDIRECT_URL (pagina a la que vuelve el cliente). La llave publica "pub_test_..." indica sandbox.
 *
 * Los precios de los planes ya incluyen el margen de la pasarela (V40), asi que el cobro es precio x meses.
 */
@Service
public class WompiService {

    public static final String MONEDA = "COP";
    static final String CHECKOUT = "https://checkout.wompi.co/p/";
    static final int MAX_MESES = 12;

    public record Cobro(String url, String referencia, long montoCentavos, int meses, boolean sandbox) {
    }

    public record Estado(boolean habilitado, boolean sandbox, BigDecimal precioMensual, BigDecimal costoPaquetes,
            BigDecimal totalMensual, List<String> faltantes) {
    }

    public record Transaccion(String referencia, String estado, int meses, long montoCentavos, String mensaje) {
    }

    private final JdbcTemplate jdbc;
    private final SuscripcionService suscripcion;
    private final PaquetesService paquetes;
    private final Function<String, String> env;

    @Autowired // dos constructores: Spring usa este; el otro (con env) es para las pruebas
    public WompiService(JdbcTemplate jdbc, SuscripcionService suscripcion, PaquetesService paquetes) {
        this(jdbc, suscripcion, paquetes, System::getenv);
    }

    WompiService(JdbcTemplate jdbc, SuscripcionService suscripcion, PaquetesService paquetes,
            Function<String, String> env) {
        this.jdbc = jdbc;
        this.suscripcion = suscripcion;
        this.paquetes = paquetes;
        this.env = env;
    }

    // ------------------------------------------------------------------ consulta

    public Estado estado(int clubId) {
        List<String> faltan = new ArrayList<>();
        if (vacio("WOMPI_PUBLIC_KEY")) faltan.add("WOMPI_PUBLIC_KEY");
        if (vacio("WOMPI_INTEGRITY_SECRET")) faltan.add("WOMPI_INTEGRITY_SECRET");
        if (vacio("WOMPI_EVENTS_SECRET")) faltan.add("WOMPI_EVENTS_SECRET");
        BigDecimal plan = precioPlan(clubId);
        BigDecimal extra = costoPaquetes(paquetes.vista(clubId));
        BigDecimal total = plan.add(extra);
        return new Estado(faltan.isEmpty() && total.signum() > 0, esSandbox(), plan, extra, total, faltan);
    }

    public Transaccion transaccionDelClub(int clubId, String referencia) {
        List<Map<String, Object>> f = jdbc.queryForList(
                "SELECT referencia, estado, meses, monto_centavos, mensaje FROM wompi_transacciones "
                        + "WHERE referencia = ? AND club_id = ?", referencia, clubId);
        if (f.isEmpty()) {
            throw new RuntimeException("Pago no encontrado");
        }
        Map<String, Object> r = f.get(0);
        return new Transaccion((String) r.get("referencia"), (String) r.get("estado"),
                ((Number) r.get("meses")).intValue(), ((Number) r.get("monto_centavos")).longValue(),
                (String) r.get("mensaje"));
    }

    // ------------------------------------------------------------------ iniciar el pago

    @Transactional
    public Cobro crearCobro(int clubId, int meses, String quien) {
        Estado e = estado(clubId);
        if (!e.faltantes().isEmpty()) {
            throw new IllegalStateException("El pago en linea todavia no esta disponible");
        }
        if (e.totalMensual().signum() <= 0) {
            throw new RuntimeException("Tu plan no tiene cobro en linea. Contacta a CheerManager.");
        }
        if (meses < 1 || meses > MAX_MESES) {
            throw new RuntimeException("Los meses deben estar entre 1 y " + MAX_MESES);
        }
        long centavos = centavos(e.totalMensual(), meses);
        String referencia = nuevaReferencia(clubId);
        jdbc.update("INSERT INTO wompi_transacciones (club_id, referencia, meses, monto_centavos, estado, creado_por, "
                + "creado_en) VALUES (?, ?, ?, ?, 'CREADA', ?, ?)", clubId, referencia, meses, centavos,
                recortar(quien, 80), LocalDateTime.now());
        String firma = firmaIntegridad(referencia, centavos, MONEDA, env.apply("WOMPI_INTEGRITY_SECRET").trim());
        return new Cobro(urlCheckout(env.apply("WOMPI_PUBLIC_KEY").trim(), centavos, referencia, firma,
                env.apply("WOMPI_REDIRECT_URL")), referencia, centavos, meses, e.sandbox());
    }

    // ------------------------------------------------------------------ webhook

    /**
     * Procesa un evento de Wompi. Lanza SecurityException si el checksum no es valido (el controlador responde 401).
     * Devuelve un texto corto con lo que paso (para el log): nunca falla por eventos que no nos interesan.
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public String procesarEvento(Map<String, Object> evento, String checksumEncabezado) {
        String secreto = env.apply("WOMPI_EVENTS_SECRET");
        if (secreto == null || secreto.isBlank()) {
            throw new IllegalStateException("Falta WOMPI_EVENTS_SECRET en el servidor");
        }
        if (!checksumValido(evento, secreto.trim(), checksumEncabezado)) {
            throw new SecurityException("Checksum invalido");
        }
        if (!"transaction.updated".equals(evento.get("event"))) {
            return "IGNORADO";
        }
        Map<String, Object> data = (Map<String, Object>) evento.get("data");
        String referencia = valorEn(data, "transaction.reference");
        String status = valorEn(data, "transaction.status");
        String wompiId = valorEn(data, "transaction.id");

        List<Map<String, Object>> filas = jdbc.queryForList(
                "SELECT id, club_id, meses, monto_centavos FROM wompi_transacciones WHERE referencia = ?", referencia);
        if (filas.isEmpty()) {
            return "SIN_REFERENCIA";
        }
        Map<String, Object> fila = filas.get(0);
        int id = ((Number) fila.get("id")).intValue();
        int clubId = ((Number) fila.get("club_id")).intValue();
        int meses = ((Number) fila.get("meses")).intValue();
        long esperado = ((Number) fila.get("monto_centavos")).longValue();

        String nuevo = estadoDe(status);
        if (nuevo == null) {
            return "EN_PROCESO"; // PENDING u otro estado intermedio: llegara otro evento
        }
        if ("APROBADA".equals(nuevo)) {
            if (!String.valueOf(esperado).equals(valorEn(data, "transaction.amount_in_cents"))
                    || !MONEDA.equals(valorEn(data, "transaction.currency"))) {
                marcar(id, "ERROR", wompiId, "El monto o la moneda del pago no coinciden con lo esperado");
                return "MONTO_NO_COINCIDE";
            }
            // El UPDATE condicionado hace que un evento repetido (Wompi reintenta) no registre dos veces.
            int n = jdbc.update("UPDATE wompi_transacciones SET estado = 'APROBADA', wompi_id = ?, mensaje = NULL, "
                    + "actualizado_en = ? WHERE id = ? AND estado <> 'APROBADA'", wompiId, LocalDateTime.now(), id);
            if (n == 0) {
                return "YA_PROCESADA";
            }
            suscripcion.registrarPago(clubId, new SuscripcionService.PagoRequest(
                    BigDecimal.valueOf(esperado, 2).setScale(0, RoundingMode.HALF_UP), meses, "WOMPI", wompiId, null,
                    "Pago en linea " + referencia), "wompi");
            return "APROBADA";
        }
        jdbc.update("UPDATE wompi_transacciones SET estado = ?, wompi_id = ?, actualizado_en = ? "
                + "WHERE id = ? AND estado <> 'APROBADA'", nuevo, wompiId, LocalDateTime.now(), id);
        return nuevo;
    }

    private void marcar(int id, String estado, String wompiId, String mensaje) {
        jdbc.update("UPDATE wompi_transacciones SET estado = ?, wompi_id = ?, mensaje = ?, actualizado_en = ? "
                + "WHERE id = ? AND estado <> 'APROBADA'", estado, wompiId, mensaje, LocalDateTime.now(), id);
    }

    // ------------------------------------------------------------------ puro (probado sin base de datos)

    /** SHA-256 hex de "referencia + monto en centavos + moneda + secreto de integridad". */
    public static String firmaIntegridad(String referencia, long centavos, String moneda, String secreto) {
        return sha256(referencia + centavos + moneda + secreto);
    }

    /**
     * Checksum que Wompi debe haber enviado: valores de data segun signature.properties (en ese orden), luego el
     * timestamp del evento y el secreto de eventos; SHA-256 hex.
     */
    @SuppressWarnings("unchecked")
    public static String checksumEsperado(Map<String, Object> evento, String secreto) {
        Map<String, Object> firma = (Map<String, Object>) evento.get("signature");
        Map<String, Object> data = (Map<String, Object>) evento.get("data");
        if (firma == null || data == null || !(firma.get("properties") instanceof List<?> props) || props.isEmpty()
                || evento.get("timestamp") == null) {
            throw new IllegalArgumentException("Evento sin firma");
        }
        StringBuilder sb = new StringBuilder();
        for (Object p : props) {
            sb.append(valorEn(data, String.valueOf(p)));
        }
        sb.append(evento.get("timestamp")).append(secreto);
        return sha256(sb.toString());
    }

    /** Compara en tiempo constante el checksum recibido (encabezado o signature.checksum) con el esperado. */
    @SuppressWarnings("unchecked")
    public static boolean checksumValido(Map<String, Object> evento, String secreto, String encabezado) {
        try {
            String recibido = encabezado;
            if (recibido == null || recibido.isBlank()) {
                Map<String, Object> firma = (Map<String, Object>) evento.get("signature");
                recibido = firma == null ? null : (String) firma.get("checksum");
            }
            if (recibido == null || recibido.isBlank()) {
                return false;
            }
            return MessageDigest.isEqual(checksumEsperado(evento, secreto).getBytes(StandardCharsets.UTF_8),
                    recibido.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Valor en una ruta con puntos ("transaction.id") dentro de mapas anidados; "" si no existe. */
    @SuppressWarnings("unchecked")
    public static String valorEn(Map<String, Object> mapa, String ruta) {
        Object actual = mapa;
        for (String parte : ruta.split("\\.")) {
            if (!(actual instanceof Map)) {
                return "";
            }
            actual = ((Map<String, Object>) actual).get(parte);
        }
        return actual == null ? "" : String.valueOf(actual);
    }

    /** Estado propio segun el status de Wompi; null = todavia en proceso. */
    public static String estadoDe(String status) {
        return switch (status == null ? "" : status) {
            case "APPROVED" -> "APROBADA";
            case "DECLINED" -> "RECHAZADA";
            case "VOIDED" -> "ANULADA";
            case "ERROR" -> "ERROR";
            default -> null;
        };
    }

    /** Pesos x meses, en centavos, sin decimales perdidos. */
    public static long centavos(BigDecimal mensual, int meses) {
        return mensual.multiply(BigDecimal.valueOf(meses)).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /** Costo mensual de los paquetes vigentes: cantidad x (precio acordado, o el precio del paquete). */
    public static BigDecimal costoPaquetes(PaquetesService.Vista v) {
        BigDecimal total = BigDecimal.ZERO;
        for (PaquetesService.Asignacion a : v.asignados()) {
            if (!a.vigente()) continue;
            BigDecimal precio = a.precioAcordado();
            if (precio == null) {
                precio = v.ofrecidos().stream().filter(p -> p.id().equals(a.paqueteId()))
                        .map(PaquetesService.Paquete::precioMensual).findFirst().orElse(BigDecimal.ZERO);
            }
            total = total.add(precio.multiply(BigDecimal.valueOf(a.cantidad())));
        }
        return total;
    }

    static String urlCheckout(String llavePublica, long centavos, String referencia, String firma, String redirect) {
        StringBuilder u = new StringBuilder(CHECKOUT).append("?public-key=").append(enc(llavePublica))
                .append("&currency=").append(MONEDA).append("&amount-in-cents=").append(centavos)
                .append("&reference=").append(enc(referencia)).append("&signature:integrity=").append(firma);
        if (redirect != null && !redirect.isBlank()) {
            String sep = redirect.contains("?") ? "&" : "?";
            u.append("&redirect-url=").append(enc(redirect.trim() + sep + "pago=" + referencia));
        }
        return u.toString();
    }

    static String nuevaReferencia(int clubId) {
        return "CM" + clubId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private BigDecimal precioPlan(int clubId) {
        BigDecimal p = suscripcion.resumen(clubId).precioMensual();
        return p == null ? BigDecimal.ZERO : p;
    }

    private boolean esSandbox() {
        String k = env.apply("WOMPI_PUBLIC_KEY");
        return k != null && k.trim().startsWith("pub_test_");
    }

    private boolean vacio(String variable) {
        String v = env.apply(variable);
        return v == null || v.isBlank();
    }

    private static String sha256(String s) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String recortar(String s, int max) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
