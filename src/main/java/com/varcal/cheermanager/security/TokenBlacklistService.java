package com.varcal.cheermanager.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Tokens revocados (logout y renovacion de sesion).
 *
 * Se consulta en CADA peticion, asi que la lectura es solo en memoria. Cada revocacion tambien se guarda en la base
 * (tabla tokens_revocados, V14) y al arrancar se vuelve a cargar: un reinicio del servidor ya no "resucita" tokens
 * cerrados. Solo se guarda el SHA-256 del token, nunca el token.
 *
 * Limite conocido: con VARIAS instancias del API cada una veria las revocaciones nuevas de las otras solo al
 * reiniciar. El despliegue actual es de una sola instancia; si se escala, la consulta debe ir a la base.
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    /** Donde se guardan las revocaciones (hash -> instante de vencimiento en ms). */
    public interface Almacen {
        void guardar(String hash, long expiraMs);

        Map<String, Long> cargarVigentes(long ahoraMs);

        void purgar(long ahoraMs);
    }

    private final Map<String, Long> revocados = new ConcurrentHashMap<>();
    private final Almacen almacen; // null = solo memoria
    private final LongSupplier reloj;

    /** Solo memoria (pruebas y arranque sin base). */
    public TokenBlacklistService() {
        this(null, System::currentTimeMillis);
    }

    @Autowired
    public TokenBlacklistService(Almacen almacen) {
        this(almacen, System::currentTimeMillis);
    }

    TokenBlacklistService(Almacen almacen, LongSupplier reloj) {
        this.almacen = almacen;
        this.reloj = reloj;
    }

    /** Recarga las revocaciones vigentes. Si la base falla se sigue en memoria (y se deja el error en el log). */
    @PostConstruct
    public void cargar() {
        if (almacen == null) {
            return;
        }
        try {
            Map<String, Long> vigentes = almacen.cargarVigentes(reloj.getAsLong());
            revocados.putAll(vigentes);
            log.info("Tokens revocados cargados desde la base: {}", vigentes.size());
        } catch (RuntimeException e) {
            log.error("No se pudieron cargar los tokens revocados; se sigue solo con memoria", e);
        }
    }

    public void blacklistToken(String token, Date expiration) {
        if (token == null || expiration == null) {
            return;
        }
        String hash = hash(token);
        long exp = expiration.getTime();
        revocados.put(hash, exp);
        if (almacen != null) {
            try {
                almacen.guardar(hash, exp);
            } catch (RuntimeException e) {
                // La revocacion ya vale en memoria; solo se perderia si el servidor se reinicia antes de que venza.
                log.error("No se pudo guardar el token revocado en la base", e);
            }
        }
    }

    public boolean isBlacklisted(String token) {
        if (token == null) {
            return false;
        }
        String hash = hash(token);
        Long exp = revocados.get(hash);
        if (exp == null) {
            return false;
        }
        if (exp <= reloj.getAsLong()) {
            revocados.remove(hash);
            return false;
        }
        return true;
    }

    /** Cada hora: olvida los tokens que ya vencieron solos (memoria y base). */
    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT10M")
    public void purgarVencidos() {
        long ahora = reloj.getAsLong();
        revocados.values().removeIf(exp -> exp <= ahora);
        if (almacen != null) {
            try {
                almacen.purgar(ahora);
            } catch (RuntimeException e) {
                log.error("No se pudieron purgar los tokens revocados vencidos", e);
            }
        }
    }

    int tamano() {
        return revocados.size();
    }

    static String hash(String token) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
